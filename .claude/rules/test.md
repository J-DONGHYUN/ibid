---
paths:
  - "src/test/**"
---

# 테스트

정본은 `docs/03-convention/test.md`. 자주 틀리는 것만 적는다.

- **테스트 목록은 `docs/01-product/requirements.md` 의 검증 기준에서 수집한다.** 새로 설계하지 않는다
- `@DisplayName` 앞에 ID 를 붙인다 — `@DisplayName("[TR-01] … 한다.")`
- **실패 케이스를 반드시 쓴다.** 성공 경로만 쓰고 끝내지 않는다
- 상태 전이는 금지된 전이까지 전부 쓴다
- 테스트에 `@Transactional` 을 붙이지 않는다. `IntegrationTestSupport` 를 상속하면 `DbCleaner` 가 정리한다
- 예외는 `assertThatThrownBy` 로, `ErrorCode` 까지 본다. JUnit assertion 을 쓰지 않는다
- 동시성 테스트는 `ProductLikeConcurrencyTest` 모양을 따른다. 성공 수만이 아니라 **최종 상태**를 본다

실행 전 — Docker 가 켜져 있고 `AWS_ACCESS_KEY` · `AWS_SECRET_KEY` 가 있어야 한다 (`CLAUDE.md` 겪은 함정).
**테스트가 실패하면 테스트를 고쳐 통과시키지 않는다.** 코드를 고친다.
