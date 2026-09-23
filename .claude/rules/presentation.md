---
paths:
  - "src/main/java/**/presentation/**"
---

# presentation 계층

HTTP 계약 · 요청/응답 DTO · 인증 표시.

- **공개 API 는 핸들러에 `@PublicApi` 를 붙인다.** `WebConfig.PUBLIC_ENDPOINTS` 에 경로를 더하지 않는다 —
  HTTP 메서드를 구분하지 못해 같은 경로의 수정 · 삭제까지 열린다 (`ADR-0003`).
  에이전트가 가장 먼저 떠올리는 방법이라 명시한다
- 로그인 사용자는 `@LoginUser UserInfo` 로 받는다. 공개 API 에서 선택적으로 받으면 `@LoginUser(required = false)`
- **상태 전이는 하위 경로 `POST`** (`/reservation` · `/completion`). `PATCH` 로 `status` 필드를 바꾸지 않는다
- DTO 는 `record`, 이름은 `~Request` · `~Response`. 엔티티를 응답으로 그대로 내보내지 않는다
- 요청 DTO 를 서비스에 그대로 넘기지 않는다. 커맨드로 바꿔 넘긴다
- 목록 API 는 커서 기반 (`ADR-0004`)

새 컨트롤러를 만들면 `ControllerTestSupport` 에 그 서비스의 `@MockitoBean` 을 더해야 컨트롤러 테스트가 뜬다.
