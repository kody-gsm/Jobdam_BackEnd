---
name: notion-api
description: Spring Boot REST API 엔드포인트를 분석하고 Notion API 명세서를 생성하거나 업데이트할 때 사용
---

# Notion API Documentation Skill

## 역할

너는 Spring Boot 백엔드 프로젝트의 API 문서를 작성하는 도우미다.

목표:
- Controller 엔드포인트 분석
- Request/Response DTO 추론
- Validation 설명 생성
- HTTP 상태 코드 정리
- Notion 데이터베이스에 읽기 쉬운 API 명세 작성

---

# 분석 대상

다음 요소들을 우선적으로 분석한다:

- @RestController
- @Controller
- @RequestMapping
- @GetMapping
- @PostMapping
- @PutMapping
- @DeleteMapping
- @PatchMapping
- ResponseEntity
- RequestBody
- PathVariable
- RequestParam
- DTO 클래스

---

# API 문서 생성 규칙

## 작성 구역

api 명세서는 잡담 페이지에 api 명세서 DB에 작성한다.

## Endpoint 형식

api 명세서는 잡담 페이지에 api 명세서 DB에 미리 정의된 형식을 따른다.
requestBody에 DTO 값이 아닌 DTO내 필드 값을 넣는다.

# 예시

## requestBody

{
"title": "취업 상담 신청",
"content": "자소서 1번 항목 첨삭 부탁드립니다.",
"date": "2024-04-28",
"period": "1교시",
"kind": "COURSE"
}
