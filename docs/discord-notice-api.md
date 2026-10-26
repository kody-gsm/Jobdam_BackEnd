# 디스코드 공지 API

모든 API는 `TEACHER` 권한이 필요합니다.

## 채널 선택 후 발송

1. `GET /api/notices/channels`로 전송 가능한 채널을 조회합니다. 응답의 각 항목은 `id`, `name`, `categoryName`을 포함합니다.
2. 선택한 채널의 `id`를 `channelId`로 사용해 `POST /api/notices/send`를 호출합니다.

```json
{
  "channelId": "123456789012345678",
  "title": "공지 제목",
  "content": "공지 내용",
  "field": "취업",
  "link": "https://example.com"
}
```

`channelId`, `title`, `content`는 필수입니다. 성공 응답은 디스코드 메시지 ID 문자열입니다. 선택 채널은 설정된 디스코드 서버에 속해야 하고 봇에 메시지 및 임베드 전송 권한이 있어야 합니다.

선택 채널에 보낸 메시지는 `PATCH /api/notices/{messageId}?channelId={channelId}`로 수정합니다. 본문은 `title`, `content`, 선택적인 `field`, `link`를 포함합니다. `channelId`를 생략하면 기존처럼 기본 공지 채널의 메시지를 수정합니다.

기존 `POST /api/notices`도 `channelId`를 보내면 선택 채널로 발송합니다. `channelId`가 없으면 기존처럼 기본 공지 채널을 사용합니다. 두 경로 모두 선택 발송 시 같은 메시지 ID 문자열을 반환합니다.

## 서버 설정

봇이 서버 하나에만 참여한 경우 기본 채널을 찾지 못해도 그 서버에서 채널 목록과 선택 전송을 처리할 수 있습니다. 봇이 여러 서버에 참여하고 기본 채널도 찾지 못하거나 특정 서버를 명시하려면 `DISCORD_BOT_GUILD_ID`를 설정합니다. 기본 채널 발송에는 별도로 `DISCORD_BOT_CHANNEL_ID`가 필요합니다.
