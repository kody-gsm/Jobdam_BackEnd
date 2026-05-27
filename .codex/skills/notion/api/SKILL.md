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

