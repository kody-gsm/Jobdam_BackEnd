# 관리자 통계 API

모든 엔드포인트는 `ROLE_ADMIN` JWT가 필요하다. 기간은 서울 날짜 기준이며 `from`, `to`를 생략하면 최근 30일이다. 최대 366일을 조회할 수 있다.

- `GET /admin/statistics/api?from=2026-10-01&to=2026-10-22`: 전체 호출 수, 4xx/5xx 호출 수, 평균 처리 시간(ms), 일별 호출, 경로·메서드·HTTP 상태별 호출 수. 인증 실패나 매핑되지 않은 요청은 `UNMATCHED` 경로로 집계한다. URL 파라미터, 요청 본문, 사용자 식별자는 저장하지 않는다.
- `GET /admin/statistics/service`: 현재 서비스 엔티티의 건수. 사용자, 예약, 공고, 폼, 제출, 알림, 상담 리마인더, 배너, 합격 보고는 상태·종류별 건수도 제공한다. 기간 필터를 적용하지 않는 현재 데이터 스냅샷이다.
- `GET /admin/statistics/discord?from=2026-10-01&to=2026-10-22`: 기본 공지(`NOTICE_DEFAULT`), 선택 채널 공지(`NOTICE_SELECTED`), DM(`DIRECT_MESSAGE`) 발송 시도와 성공·실패 건수. 메시지 수정·삭제와 과거 발송은 포함하지 않는다.

운영 배포 전 [statistics-migration.sql](statistics-migration.sql)을 실행한다. 집계는 코드 배포 이후부터 시작하며 과거 API 호출과 디스코드 발송을 복원할 수 없다. 서비스 데이터 건수는 기존 테이블을 즉시 조회한다.
