# 프론트: SSE 알림 재연결

백엔드가 블루-그린 무중단 배포(`docs/zero-downtime-deploy.md`)로 바뀌었다.
배포할 때마다 옛 컨테이너가 내려가면서 **SSE 연결이 서버 쪽에서 닫힌다.** 프론트가 알아서 다시 붙어야 알림이 계속 온다.
배포가 아니어도 서버 재시작, 네트워크 끊김, 1시간 타임아웃 때 같은 일이 생기므로 이 처리는 원래 필요했다.

## 지금 구독 방식

1. `POST /api/notifications/subscribe-ticket` (JWT) → `{ ticket, expiresAt }`
   - 티켓은 **60초 동안 한 번만** 쓸 수 있다. 서버 메모리에 있어서 서버가 바뀌면 사라진다.
2. `GET /api/notifications/subscribe?ticket=...` → `text/event-stream`
3. 받는 이벤트
   - `connect`: 연결 직후 한 번
   - `heartbeat`: 30초마다 (`ping`)
   - `notification`: 새 알림 (`NotificationResponseDTO`, DB에 저장된 알림)
   - `reservation`: 상담 예약 상태 실시간 갱신 (DB에 따로 남지 않음)

## 꼭 해야 하는 것

### 1. 끊기면 새 티켓으로 다시 연결한다

브라우저 `EventSource`는 끊기면 **같은 URL로** 자동 재연결한다. 그런데 URL의 티켓은 이미 썼으므로 서버가 `400`을 준다.
`EventSource`는 200이 아닌 응답을 받으면 **재연결을 영원히 멈춘다**(`readyState === CLOSED`).
그래서 `onerror`에서 직접 닫고, 티켓을 새로 받아 다시 연결해야 한다.

### 2. 다시 연결되면 놓친 것을 다시 불러온다

전환하는 몇 초 동안 생긴 알림은 실시간으로 오지 않는다. 알림은 DB에 있으므로 다시 조회하면 채워진다.

- `connect` 이벤트를 받을 때마다 안 읽은 알림 목록·개수를 다시 조회한다.
- `reservation` 이벤트는 DB에 남지 않으므로, 상담 예약 화면이 열려 있으면 그 화면 데이터를 다시 조회한다.

### 3. 재연결을 조금씩 늦춘다

배포 순간 모든 사용자가 동시에 끊긴다. 바로 다 같이 재연결하면 새 컨테이너에 요청이 몰린다.
1~3초 사이 무작위로 기다리고, 계속 실패하면 간격을 늘린다(최대 30초 정도).

## 예시

```js
let es = null;
let retry = 0;
let timer = null;

async function connectNotifications() {
  clearTimeout(timer);
  try {
    const { ticket } = await api.post('/api/notifications/subscribe-ticket'); // JWT가 붙는 기존 API 클라이언트
    es = new EventSource(`/api/notifications/subscribe?ticket=${encodeURIComponent(ticket)}`);
  } catch (e) {
    // 401이면 토큰 갱신 흐름을 탄다. 그 외에는 잠시 뒤 다시 시도한다.
    return scheduleReconnect();
  }

  es.addEventListener('connect', () => {
    retry = 0;
    refetchUnreadNotifications();   // 끊긴 동안 놓친 알림 채우기
    refetchReservationsIfVisible(); // reservation 이벤트는 DB에 없으므로 화면 다시 조회
  });
  es.addEventListener('notification', (e) => onNotification(JSON.parse(e.data)));
  es.addEventListener('reservation', (e) => onReservation(JSON.parse(e.data)));
  // heartbeat는 따로 처리하지 않아도 된다.

  es.onerror = () => {
    es.close();          // 옛 티켓으로 자동 재연결하지 않게 막는다
    scheduleReconnect();
  };
}

function scheduleReconnect() {
  const base = Math.min(30_000, 1_000 * 2 ** retry); // 1s, 2s, 4s ... 최대 30s
  retry += 1;
  timer = setTimeout(connectNotifications, base + Math.random() * 2_000);
}

// 로그아웃하면 끊는다. 다시 로그인하면 connectNotifications()
function disconnectNotifications() {
  clearTimeout(timer);
  es?.close();
  es = null;
  retry = 0;
}
```

- `EventSourcePolyfill`이나 `fetch`로 직접 SSE를 읽고 있다면 원리는 같다. 끊기면 **새 티켓 → 새 연결 → `connect`에서 다시 조회**.
- 탭이 백그라운드에서 돌아왔을 때(`visibilitychange`) 연결이 닫혀 있으면 바로 `connectNotifications()`를 불러도 좋다.

## 프론트 Nginx

`/api/notifications/subscribe`를 프록시하는 위치에서 확인한다. SSE가 지금 잘 오면 대부분 이미 맞춰져 있다.

```nginx
proxy_http_version 1.1;
proxy_set_header Connection "";
proxy_buffering off;        # 백엔드가 X-Accel-Buffering: no도 보내지만 명시해 두면 확실하다
proxy_read_timeout 3600s;   # heartbeat(30초)보다 길어야 한다. 기본 60초도 동작은 하지만 여유가 적다
```

## 확인 방법

1. 알림 화면을 열어 둔 채로 백엔드를 배포한다. 또는 개발 서버를 재시작한다.
2. 개발자 도구 Network 탭에서 `subscribe-ticket` → `subscribe`가 다시 호출되는지 본다.
3. 배포 중에 다른 계정으로 상담을 신청하고, 다시 연결된 뒤 알림 목록에 그 알림이 보이는지 본다.
