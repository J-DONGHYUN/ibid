# 테스트 컨벤션

이전 `TEST.md`(작성: wlsh44)의 규칙을 바탕으로, 요구사항 연결과 필수 테스트를 더했다.

## 무엇을 테스트하나

**테스트 목록은 설계하지 않고 수집한다.** `requirements.md` 의 검증 기준 한 줄이 테스트 하나 이상이다.
그리고 아래 넷은 검증 기준에 없어도 반드시 쓴다.

| 분류 | 대상 | 방식 |
|---|---|---|
| 상태 전이 | 허용된 전이와 **금지된 전이 모두** (`domain-model.md` 전이표의 칸마다) | 단위 테스트 |
| 동시성 | 유니크 제약 · 상태 전이 · 멱등 요청 | 통합 테스트 + 멀티스레드 |
| 권한 | 요청자에 따라 결과가 달라지는 곳 (본인 · 남 · 비로그인) **모든 조합** | 통합 · 컨트롤러 테스트 |
| 경계 | 커서 페이지네이션 (정렬 키가 같은 데이터를 일부러 만든다) | 통합 테스트 |

## 요구사항과 연결

- [G] `@DisplayName` 앞에 요구사항 · 불변식 ID 를 붙인다 — `@DisplayName("[TR-01] 판매중인 상품을 채팅 상대에게 예약한다.")`
- 이렇게 두면 "이 ID 의 검증 기준에 테스트가 있는가"를 기계가 셀 수 있다 (하네스에서 쓴다)

## 이름과 구조

- 메서드명은 영어. 실패 케이스는 `{메서드명}_{실패이유}` (`reserve_alreadyReserved`)
- [G] `@Test` 에는 `@DisplayName` 을 붙인다. 사용자 행위 · 규칙 중심으로 쓰고 어투는 `~다.`
- given · when · then 으로 나누고 **when 은 한 번만** 실행한다. 두 번 나오면 테스트를 나눈다
- 테스트를 이해하는 데 필요 없는 값은 fixture · builder 로 숨긴다. 중요한 값은 본문에 드러낸다

## 검증

- [G] AssertJ 를 쓴다. JUnit 의 `assertEquals` · `assertThrows` 를 쓰지 않는다
- 예외는 `assertThatThrownBy` 로 검증하고 **타입만 보지 말고 `ErrorCode` 까지** 본다

## 계층별

| 테스트 | 기반 클래스 | 무엇을 |
|---|---|---|
| 단위 | 없음 | 도메인 규칙 · 상태 전이 |
| 통합 | `IntegrationTestSupport` | 유스케이스 흐름 · 트랜잭션 · 동시성. Testcontainers MySQL · Redis |
| 저장소 | `RepositoryTestSupport` | 쿼리 조건 · 정렬 · 매핑 |
| 컨트롤러 | `ControllerTestSupport` | HTTP 계약 (경로 · 상태 코드 · 인증 등급 · 응답 형식). 서비스는 mock |

- [G] **테스트에 `@Transactional` 을 붙이지 않는다.** DB 정리는 `DbCleaner` 가 매 테스트 전에 한다.
  트랜잭션 롤백 방식이면 동시성 테스트가 성립하지 않는다 (다른 스레드가 커밋 전 데이터를 못 본다)
- H2 로 테스트하지 않는다. MySQL 과 문법 · 락 동작이 다르다
- Mock 은 외부 협력(S3 · RabbitMQ · 시간 · 랜덤)과 실패 강제에만 쓴다. 도메인 객체를 mock 하지 않는다

## 동시성 테스트 모양

`ProductLikeConcurrencyTest` 가 기준이다.

```java
@DisplayName("[TR-01] 서로 다른 두 상대로 동시에 예약해도 한 명만 예약된다.")
@Test
void reserve_concurrentRequestsReserveOnlyOne() throws InterruptedException {
    // given — 상품 하나, 채팅 상대 둘
    // when  — ExecutorService + CountDownLatch 로 동시에 시작
    // then  — 성공 수 · 실패 수 · 최종 상태를 모두 본다
}
```

- 성공 수만 보지 말고 **최종 상태**(예약 상대가 누구인지, 하나뿐인지)까지 본다
