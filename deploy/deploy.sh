#!/usr/bin/env bash
# 블루-그린 무중단 배포 (docs/zero-downtime-deploy.md)
#
#   bash deploy/deploy.sh            새 이미지를 쉬고 있는 색에 띄우고 전환한다. (GitHub Actions가 실행)
#   bash deploy/deploy.sh rollback   멈춰 둔 이전 색 컨테이너를 다시 켜고 전환한다. (이미지를 받지 않는다)
#
# 순서: 프록시 설정 검사 → 새 색 기동 → 헬스체크 통과 대기 → 프록시 전환(nginx reload) → 잠깐 기다림 → 옛 색 graceful 종료
# 헬스체크에 실패하면 새 색을 내리고 멈춘다. 이때 옛 색은 그대로 서비스 중이다.
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE=(docker compose -f docker-compose.prod.yml)
# docker-compose.prod.yml의 proxy 이미지와 같아야 한다. 새 설정을 미리 검사할 때 쓴다.
PROXY_IMAGE=nginx:1.27-alpine

# GitHub Actions가 올리는 프록시 설정. 바로 쓰지 않고 검사한 뒤 LIVE_DIR로 옮긴다.
# 그래야 잘못된 설정이 올라와도 프록시가 재시작할 때 읽는 파일은 마지막으로 통과한 설정으로 남는다.
INCOMING_CONF=deploy/nginx/default.conf
LIVE_DIR=deploy/nginx-live                 # proxy 컨테이너의 /etc/nginx/conf.d
ACTIVE_FILE="$LIVE_DIR/active_backend"     # 서비스 중인 색 (set $backend ...)
STANDBY_FILE=deploy/.standby_color          # 되돌릴 수 있는(정상이던 이전 버전을 든) 멈춘 색

HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-300}"   # 새 컨테이너가 healthy가 될 때까지 기다리는 최대 초
DRAIN_SECONDS="${DRAIN_SECONDS:-10}"      # 전환 후 옛 색에 남은 요청이 끝나길 기다리는 초
STOP_TIMEOUT="${STOP_TIMEOUT:-60}"        # 옛 색 docker stop 대기 초 (스프링 graceful 30초보다 길게)

MODE="${1:-deploy}"
if [[ "$MODE" != "deploy" && "$MODE" != "rollback" ]]; then
  echo "사용법: bash deploy/deploy.sh [rollback]" >&2
  exit 2
fi

log() { echo "[deploy] $*"; }
abort() { log "중단: $*"; exit 1; }

# 배포가 동시에 두 번 돌지 않게 막는다.
if command -v flock >/dev/null 2>&1; then
  exec 9>deploy/.deploy.lock
  flock -n 9 || abort "다른 배포가 진행 중입니다."
fi

mkdir -p "$LIVE_DIR"

read_active() {
  [[ -f "$ACTIVE_FILE" ]] || return 0
  sed -nE 's#.*http://app-(blue|green):8088.*#\1#p' "$ACTIVE_FILE" | head -n 1
}

active_line() { printf 'set $backend http://app-%s:8088;\n' "$1"; }

current="$(read_active)"
if [[ "$current" == "blue" ]]; then next="green"; else next="blue"; fi
log "모드: $MODE, 서비스 중: ${current:-없음} → 전환 대상: $next"

if [[ "$MODE" == "rollback" ]]; then
  # 직전 배포가 실패했다면 멈춘 색에는 실패한 새 버전이 들어 있다. 그걸 켜면 되돌리기가 아니다.
  [[ -n "$current" && "$(cat "$STANDBY_FILE" 2>/dev/null || true)" == "$next" ]] \
    || abort "되돌릴 정상 버전이 없습니다. (첫 배포이거나 직전 배포가 실패해 app-$next에 실패한 버전이 있습니다)"
fi

# 1) 올라온 프록시 설정을 버리는 nginx 컨테이너로 먼저 검사한다. 아직 아무것도 바꾸지 않았으므로 실패하면 그냥 멈춘다.
check_dir="$(mktemp -d)"
cp "$INCOMING_CONF" "$check_dir/default.conf"
active_line "$next" > "$check_dir/active_backend"
if ! docker run --rm -v "$check_dir:/etc/nginx/conf.d:ro" "$PROXY_IMAGE" nginx -t -q; then
  rm -rf "$check_dir"
  abort "$INCOMING_CONF 설정 검사에 실패했습니다. 서비스 중인 프록시 설정은 바꾸지 않았습니다."
fi
rm -rf "$check_dir"

fail() {
  log "실패: $*"
  "${COMPOSE[@]}" logs --tail 200 "app-$next" || true
  "${COMPOSE[@]}" stop "app-$next" || true
  if [[ "$MODE" == "deploy" ]]; then
    # 멈춘 색에는 이제 실패한 버전이 있으므로 rollback 대상이 아니다.
    rm -f "$STANDBY_FILE"
  fi
  log "app-$next를 내렸습니다. 서비스는 ${current:+app-$current로 }그대로 유지됩니다."
  exit 1
}

# 2) DB가 떠 있고 healthy인지 확인한다. 설정이 같으면 아무것도 바꾸지 않는다.
"${COMPOSE[@]}" up -d --wait db

# 3) 쉬고 있는 색에 새 버전을 띄운다.
if [[ "$MODE" == "deploy" ]]; then
  "${COMPOSE[@]}" pull "app-$next"
  "${COMPOSE[@]}" up -d --no-deps --force-recreate "app-$next"
else
  # 되돌리기는 멈춰 둔 컨테이너를 그대로 켠다. 컨테이너가 옛 이미지를 들고 있다.
  "${COMPOSE[@]}" start "app-$next" || fail "멈춰 둔 app-$next 컨테이너가 없습니다."
fi

cid="$("${COMPOSE[@]}" ps -q "app-$next")"
[[ -n "$cid" ]] || fail "app-$next 컨테이너를 찾지 못했습니다."

# 4) 헬스체크(/actuator/health/readiness)가 통과할 때까지 기다린다.
log "app-$next 헬스체크 대기 (최대 ${HEALTH_TIMEOUT}초)"
deadline=$((SECONDS + HEALTH_TIMEOUT))
while :; do
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$cid")"
  case "$status" in
    healthy) break ;;
    unhealthy) fail "app-$next 헬스체크가 실패했습니다." ;;
    none) fail "app-$next에 healthcheck가 없습니다. docker-compose.prod.yml을 확인하세요." ;;
  esac
  [[ "$(docker inspect -f '{{.State.Running}}' "$cid")" == "true" ]] || fail "app-$next가 종료됐습니다."
  (( SECONDS < deadline )) || fail "${HEALTH_TIMEOUT}초 안에 healthy가 되지 않았습니다."
  sleep 5
done
log "app-$next healthy"

# 5) 처음 한 번: 블루-그린 이전의 단일 app 컨테이너를 정리한다.
project="$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$cid")"
legacy="$(docker ps -aq \
  --filter "label=com.docker.compose.project=$project" \
  --filter "label=com.docker.compose.service=app")"
if [[ -n "$legacy" ]]; then
  # 배너 이미지는 예전에 볼륨이 없어 컨테이너 안에만 있었다. 컨테이너를 지우기 전에 새 볼륨으로 옮긴다.
  for old in $legacy; do
    tmp="$(mktemp -d)"
    if docker cp "$old:/app/uploads/banner/." "$tmp/" 2>/dev/null; then
      docker cp "$tmp/." "$cid:/app/uploads/banner/"
      log "옛 컨테이너($old)의 배너 이미지를 banner-images 볼륨으로 옮겼습니다."
    fi
    rm -rf "$tmp"
  done

  # 옛 컨테이너가 8088을 쥐고 있어서 프록시를 띄우려면 먼저 내려야 한다. 처음 한 번만 몇 초 끊긴다.
  log "블루-그린 이전의 app 컨테이너를 내립니다."
  docker stop -t "$STOP_TIMEOUT" $legacy >/dev/null
  docker rm $legacy >/dev/null
fi

# 6) 프록시를 새 색으로 돌린다.
proxy_running="$("${COMPOSE[@]}" ps -q --status running proxy || true)"
if [[ -n "$proxy_running" ]]; then
  # 프록시 컨테이너 안에서 새 색에 닿는지 먼저 본다. 도커 DNS·네트워크 문제를 전환 전에 잡는다.
  "${COMPOSE[@]}" exec -T proxy wget -q -T 5 -O /dev/null "http://app-$next:8088/actuator/health/readiness" \
    || fail "프록시에서 app-$next에 연결하지 못했습니다."
fi

# 지금 설정을 남겨 두고 새 설정을 넣는다. 검사에 실패하면 둘 다 되돌린다.
backup_dir="$(mktemp -d)"
cp -p "$LIVE_DIR/default.conf" "$backup_dir/" 2>/dev/null || true
cp -p "$ACTIVE_FILE" "$backup_dir/" 2>/dev/null || true
restore_live() {
  if [[ -f "$backup_dir/default.conf" ]]; then cp -p "$backup_dir/default.conf" "$LIVE_DIR/"; else rm -f "$LIVE_DIR/default.conf"; fi
  if [[ -f "$backup_dir/active_backend" ]]; then cp -p "$backup_dir/active_backend" "$LIVE_DIR/"; else rm -f "$ACTIVE_FILE"; fi
}

cp "$INCOMING_CONF" "$LIVE_DIR/default.conf.tmp" && mv "$LIVE_DIR/default.conf.tmp" "$LIVE_DIR/default.conf"
active_line "$next" > "$ACTIVE_FILE.tmp" && mv "$ACTIVE_FILE.tmp" "$ACTIVE_FILE"

if [[ -n "$proxy_running" ]]; then
  if ! "${COMPOSE[@]}" exec -T proxy nginx -t -q || ! "${COMPOSE[@]}" exec -T proxy nginx -s reload; then
    restore_live
    rm -rf "$backup_dir"
    fail "프록시 설정을 적용하지 못했습니다. 이전 설정으로 되돌렸습니다."
  fi
fi
rm -rf "$backup_dir"
# 프록시가 없으면 띄우고, 프록시 정의가 바뀌었을 때만 다시 만든다. 같으면 아무것도 하지 않는다.
"${COMPOSE[@]}" up -d --no-deps proxy
log "프록시 → app-$next"

# 7) 옛 색을 graceful하게 내린다. 컨테이너는 지우지 않고 멈춰 둔다(rollback에 쓴다).
if [[ -n "$current" ]]; then
  log "${DRAIN_SECONDS}초 뒤 app-$current를 내립니다."
  sleep "$DRAIN_SECONDS"
  "${COMPOSE[@]}" stop -t "$STOP_TIMEOUT" "app-$current"
  printf '%s\n' "$current" > "$STANDBY_FILE"
else
  rm -f "$STANDBY_FILE"
fi

log "완료: app-$next 서비스 중"
