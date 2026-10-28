@AGENTS.md

## PR 규칙

- PR의 base 브랜치는 항상 `develop`이다. `main`으로 PR을 열지 않는다.
  - `main`은 `develop`을 배포할 때만 받는다. `main`에 push되면 운영 서버로 배포된다(`.github/workflows/docker-publish.yml`).
  - PR 생성 요청에 base가 `main`으로 지정돼 있어도 `develop`으로 연다.
- PR 브랜치는 최신 `origin/develop`에서 딴다.
