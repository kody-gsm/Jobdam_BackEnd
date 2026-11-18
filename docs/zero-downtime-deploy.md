# 블루-그린 무중단 배포

`main`에 push하면 `.github/workflows/docker-publish.yml`이 이미지를 만들고, 서버에서 `deploy/deploy.sh`를 실행한다.
예전처럼 컨테이너를 내렸다가 다시 띄우지 않으므로, 배포 중에도 서비스가 끊기지 않는다.

## 구조

```
프론트 Nginx ──▶ proxy (:8088, nginx) ──▶ app-blue  ┐ 둘 중 하나만 서비스
                                     └─▶ app-green ┘ 같은 DB, 같은 업로드 볼륨
```

- 바깥에서 보면 예전과 똑같이 `8088`이다. 프론트 Nginx 설정은 바꿀 필요가 없다.
- 어느 색이 서비스 중인지는 서버의 `deploy/nginx-live/active_backend` 한 줄이 정한다. (`set $backend http://app-blue:8088;`)
  이 파일은 `deploy.sh`가 만들고, 저장소에는 없다.
- `app-blue`와 `app-green`은 `docker-compose.prod.yml`의 `x-app` 설정을 같이 쓴다. 환경 변수를 추가할 때는 `x-app`에 한 번만 적는다.

## 배포 순서 (`deploy.sh`)

0. 올라온 `deploy/nginx/default.conf`를 버리는 nginx 컨테이너로 `nginx -t` 검사한다. 실패하면 아무것도 바꾸지 않고 멈춘다.
   프록시가 실제로 읽는 설정은 검사를 통과해 `deploy/nginx-live/`로 옮겨진 것뿐이다.
   그래서 잘못된 설정이 올라와도, 서버 재부팅으로 프록시가 다시 뜰 때 마지막으로 통과한 설정을 읽는다.
1. 쉬고 있는 색(예: green)에 새 이미지를 띄운다.
2. 컨테이너 healthcheck(`/actuator/health/readiness`)가 `healthy`가 될 때까지 기다린다. 최대 300초다.
   readiness는 기동 작업이 끝나고 DB에 연결돼야 UP이 된다.
   - 실패하면 green을 내리고 끝낸다. blue는 그대로 서비스 중이다. GitHub Actions는 실패로 표시된다.
3. 프록시 설정을 green으로 바꾸고 `nginx -s reload`를 실행한다. reload는 연결을 끊지 않는다.
4. 10초 기다린 뒤 blue를 `docker stop`으로 내린다.
   스프링은 graceful shutdown으로 처리 중인 요청을 끝낸다. 열린 SSE 연결은 먼저 닫아서 프론트가 green으로 다시 붙게 한다.
5. blue 컨테이너는 지우지 않고 멈춰 둔다. 되돌릴 때 쓴다.

## 되돌리기

새 버전에 문제가 있으면 서버의 `APP_DIR`에서 실행한다.

```bash
bash deploy/deploy.sh rollback
```

멈춰 둔 이전 색 컨테이너(이전 이미지)를 다시 켜고, healthy가 되면 프록시를 돌린다.
직전 배포가 헬스체크에서 실패했다면 멈춘 색에는 실패한 새 버전이 들어 있으므로 되돌리기를 거부한다(`deploy/.standby_color`로 판단).
이때는 서비스 중인 색이 이미 정상 버전이니 되돌릴 필요가 없다.
DB 스키마를 이미 옛 버전과 맞지 않게 바꿨다면 되돌릴 수 없다. 아래 규칙을 지켜야 하는 이유다.

## 처음 한 번 (블루-그린 전환 배포)

이 구조가 처음 배포될 때 `deploy.sh`가 알아서 처리한다.

- 예전 단일 `app` 컨테이너가 있으면 `app-blue`가 healthy가 된 뒤 내린다.
  예전 컨테이너가 8088을 쥐고 있어서, 프록시가 뜰 때까지 **이때만 몇 초 끊긴다.** 사용자가 적은 시간에 머지한다.
- 배너 이미지는 예전에 볼륨이 없어 컨테이너 안에만 있었다. 예전 컨테이너를 지우기 전에 `banner-images` 볼륨으로 복사한다.
- 예약 작업 잠금 테이블 `shedlock`은 앱이 기동할 때 없으면 만든다(`CREATE TABLE IF NOT EXISTS`). 따로 SQL을 실행하지 않아도 된다.

첫 배포 전에 서버에서 확인할 것:

- 메모리: 배포하는 1~3분 동안 JVM 두 개가 함께 뜬다. `free -m`으로 앱 하나 분량의 여유가 있는지 본다.
- docker compose 버전: `up --wait`를 쓰므로 v2.17 이상이어야 한다. `docker compose version`으로 확인한다.
- 프론트 Nginx가 `/api/notifications/subscribe`를 버퍼링하지 않는지 본다. 프록시가 `X-Accel-Buffering: no`를 붙여 주지만,
  `proxy_read_timeout`은 앞단 설정이 따로 적용되므로 heartbeat(30초)보다 길어야 한다.

## 두 버전이 함께 뜨는 동안 지켜야 할 것

전환하는 몇 초~몇 분 동안 옛 버전과 새 버전이 **같은 DB와 같은 업로드 볼륨**을 쓴다.

### 예약 작업 (@Scheduled)

모든 `@Scheduled` 작업에 `@SchedulerLock`을 붙인다. 잠금은 DB의 `shedlock` 테이블에 있어서, 두 컨테이너 중 한쪽만 실행한다.

```java
@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
@SchedulerLock(name = "작업-이름", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
public void run() { ... }
```

- `lockAtLeastFor`: 작업이 빨리 끝나도 이 시간 동안은 잠금을 쥔다. 다른 컨테이너가 같은 분에 조금 늦게 깨어나 한 번 더 도는 것을 막는다.
  1분마다 도는 작업은 1분보다 짧게(`PT20S`) 둔다.
- `lockAtMostFor`: 컨테이너가 죽어도 이 시간이 지나면 잠금이 풀린다. 작업이 걸리는 시간보다 넉넉히 둔다.
- 기동할 때 한 번 도는 작업(`@EventListener(ApplicationReadyEvent.class)`)도 같은 `name`으로 잠근다.
  기동 중에 컨테이너가 죽으면 잠금이 `lockAtMostFor`까지 남아, 그 사이 다시 배포하면 기동 작업이 건너뛰어진다(다음 cron에는 돈다).
  그래서 기동 작업을 잠그는 경우 `lockAtMostFor`를 작업 시간보다 조금 긴 정도(`PT10M`)로 짧게 둔다.
- 컨테이너마다 따로 돌아야 하는 작업(예: SSE heartbeat)만 잠그지 않는다. `ScheduledJobLockTest`의 예외 목록에 이유와 함께 넣는다.
  잠금을 빠뜨리면 이 테스트가 실패한다.

### DB 스키마 (`docs/*.sql`)

운영은 `ddl-auto=validate`다. 마이그레이션 SQL은 배포 **전에** 실행하므로, 실행한 순간부터 전환이 끝날 때까지 **옛 버전도 새 스키마 위에서 돈다.**
그래서 마이그레이션은 옛 버전과 새 버전 모두에서 동작해야 한다.

| 바꾸려는 것 | 배포 전 SQL | 앱 배포 | 배포 후 SQL (롤백할 일이 없을 때) |
|---|---|---|---|
| 컬럼·테이블 추가 | 추가 (`NULL` 허용 또는 `DEFAULT`) | 새 컬럼 사용 | - |
| 컬럼 삭제 | `NOT NULL`이면 `NULL` 허용으로 | 엔티티에서 필드 제거 | `DROP COLUMN` |
| 컬럼 이름 변경 | 새 컬럼 추가 + 복사 | 새 컬럼만 사용 | 전환 중 옛 버전이 쓴 값 다시 복사 → 옛 컬럼 `DROP` |
| `NOT NULL` 추가 | 빈 값 채우기 | 항상 값을 넣는 코드 | `NOT NULL` 추가 |
| enum 값 추가 | DB `ENUM`에 값 추가 | Java enum에 값 추가 | - |
| enum 값 삭제 | - | 그 값을 더 이상 만들지 않음 | 데이터 변경 → DB `ENUM`에서 제거 |

- 앱 배포는 한 번이다. "배포 후 SQL"은 새 버전만 남은 뒤 실행하는 SQL이다.
- enum 값을 추가하고 바로 저장하면, 그 행을 옛 버전이나 되돌린 버전이 읽을 때 `No enum constant` 예외가 난다.
  되돌릴 가능성까지 막으려면 "enum만 추가하는 배포 → 실제로 저장하는 배포"로 나눈다.
- 좋은 예: `docs/recruit-multi-image-migration.sql` (새 테이블만 만들고 옛 컬럼 `DROP`은 주석으로 미뤄 둠)
- 피할 예: 배포 전 SQL에서 옛 버전이 쓰는 컬럼을 `DROP`하거나 이름을 바꾸는 것.
  `docs/pass-report-interview-only-migration.sql`처럼 한 번에 지우면, SQL을 실행한 순간부터 전환이 끝날 때까지 옛 버전의 기능이 에러를 낸다.

### 업로드 파일

프로필·채용 공고·폼 파일·배너는 이름 있는 볼륨(`profile-images`, `recruit-images`, `form-files`, `banner-images`)에 저장한다.
blue와 green이 같은 볼륨을 붙이므로, 어느 쪽이 서비스해도 같은 파일을 본다.
새 업로드 경로를 추가할 때는 `x-app`의 환경 변수와 `volumes`에 함께 넣고, 맨 아래 `volumes:`에 볼륨 이름을 추가한다.
볼륨에 넣지 않으면 컨테이너를 새로 만들 때마다 파일이 사라진다.

### 메모리에만 있는 상태

컨테이너 메모리에 있는 상태는 다른 색에서 보이지 않는다.

- SSE 연결·구독 티켓(`NotificationSseService`, `SseTicketService`): 전환하면 연결이 끊기고 옛 티켓은 쓸 수 없다.
  프론트가 새 티켓으로 다시 연결해야 한다. → `docs/frontend-sse-reconnect.md`
- 나이스 시간표·학사일정 캐시(`TimetableService`, `ScheduleService`): 새 컨테이너가 다시 채운다. 문제없다.

새로 메모리 상태를 만들 때는, 두 컨테이너가 함께 떠 있거나 컨테이너가 바뀌어도 괜찮은지 먼저 본다.
