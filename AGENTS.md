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

## 기타

- Gradle 기준으로 작업
- 한국어 설명 선호
- 자동 커밋 및 자동 푸시 금지