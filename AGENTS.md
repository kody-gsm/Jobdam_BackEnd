# AGENTS.md

## 기술 스택

- Java 21
- Spring Boot 3
- Gradle 사용
- Spring Data JPA 사용
- Mysql DB 사용

## 패키지 구조

- 기능(feature) 기반 패키지 구조 사용
- 각 기능 내부에 controller/service/repository/dto/entity 구성

## 코드 규칙

- Controller에는 비즈니스 로직 작성 금지
- Entity 직접 반환 금지
- Request DTO / Response DTO 분리
- Service 계층에서 비즈니스 로직 처리

## Entity 규칙

- Lombok 사용 허용
- Setter 사용 최소화
- 상태값은 enum 사용

## 무중단 배포 규칙

운영은 블루-그린 배포라 옛 버전과 새 버전이 잠깐 같은 DB·볼륨을 함께 쓴다. 자세한 내용은 `docs/zero-downtime-deploy.md`.

- `@Scheduled` 작업에는 `@SchedulerLock`을 붙인다 (`ScheduledJobLockTest`가 검사)
- 마이그레이션 SQL(`docs/*.sql`)은 옛 버전에서도 동작해야 한다
  - 배포 전 SQL은 추가만 한다. 컬럼 삭제·이름 변경은 새 버전 배포가 끝난 뒤 실행할 SQL로 따로 둔다
- 업로드 파일은 `docker-compose.prod.yml`의 이름 있는 볼륨에 저장한다

## 기타

- Gradle 기준으로 작업
- 모든 응답은 한국어
- 자동 커밋 및 자동 푸시 금지
- 커밋 메시지와 PR 본문에 AI 도구 흔적을 남기지 않는다
  - `Co-Authored-By: Claude ...` 서명 금지
  - `🤖 Generated with Claude Code` 등 푸터 금지
  - 세션 링크(`claude.ai/code/...`) 첨부 금지
  - 커밋 메시지는 변경 내용만 한국어 Conventional Commits 형식으로 작성
