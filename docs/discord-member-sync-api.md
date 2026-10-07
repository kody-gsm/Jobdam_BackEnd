# 디스코드 멤버 수동 동기화 API

관리자 화면의 버튼에서 아래 API를 호출하면 디스코드 서버 멤버 전체를 조회하고 학생 계정의 디스코드 ID 연결을 갱신합니다. 동기화가 끝난 뒤 응답합니다.

```http
POST /admin/discord/members/sync
Authorization: Bearer <관리자 JWT>
```

성공 응답 (`200 OK`):

```json
{
  "scannedMembers": 300,
  "linkedStudents": 280,
  "updatedStudents": 2
}
```

- `scannedMembers`: 조회한 서버 멤버 수
- `linkedStudents`: 동기화 후 디스코드 ID가 연결된 학생 수
- `updatedStudents`: 이번 요청에서 연결 값이 바뀐 학생 수

기존 `/admin/**` 보안 규칙에 따라 관리자 권한이 필요합니다. 봇의 공지 채널이 속한 서버를 조회합니다.

자동 동기화는 서버 시작 시와 매일 04:00(Asia/Seoul)에 수행합니다. 멤버 입장, 닉네임 변경, 퇴장 이벤트는 영향을 받는 학생만 갱신합니다. 봇의 Guild Members 인텐트가 필요합니다.
