# EXP-01 상품 상태 전이 동시성 — 낙관적 락 vs 비관적 락

- 관련: `ADR-0008` · `I-12` · `I-01` · `T-20`
- 측정일: 2026-10-01
- 목적: 한 상품에 동시 예약이 들어올 때 표에 없는 상태(Lost Update)가 생기지 않는지, 그리고 두 락 방식의 비용을 비교해 `ADR-0008` 을 근거로 정한다

## 방법

- `Testcontainers` MySQL, `ProductReservationConcurrencyTest`
- 판매중 상품 하나에 **20개 스레드가 서로 다른 상대(buyerId)로 동시에 `reserve`** 를 호출
- 같은 `product.reserve()` 전이(판매중→예약중)를 두 경로로 호출:
  - 낙관적: `findById` + `@Version` (커밋 시 버전 충돌이면 실패)
  - 비관적: `findByIdForUpdate`(`SELECT … FOR UPDATE`)
- 기대: 정확히 1건 성공, 나머지 실패, 최종 상태는 예약중·예약 상대 한 명 (`I-12` · `I-01`)

## 결과

| 방식 | 스레드 | 성공 | 실패 | 실패 유형 | 소요 |
|---|---|---|---|---|---|
| 낙관적 락 (`@Version`) | 20 | 1 | 19 | `ObjectOptimisticLockingFailureException` 9 · `BusinessException`(판매중 아님) 10 | 26ms |
| 비관적 락 (`FOR UPDATE`) | 20 | 1 | 19 | `BusinessException`(판매중 아님) 19 | 62ms |

- **정확성: 둘 다 `I-12` 를 만족.** 최종 상태는 예약중, 예약 상대 한 명.
- **비용: 낙관적이 2배 이상 빠르다(26 vs 62ms).** 비관적은 20개 요청이 행 락에 직렬화돼 대기한다. 낙관적은 대기가 없고, 진 쪽만 커밋 시점(버전 충돌) 또는 읽기 시점(이미 예약중)에 실패한다.
- **실패 유형: 낙관적은 두 가지**(버전 충돌 + 전이 가드), **비관적은 한 가지**(전이 가드). 낙관적은 예외 핸들러가 `OptimisticLockingFailure → 409` 매핑을 해줘야 한다.

## 해석 · 결정

`ADR-0008` 의 결정 기준에 비춘다:

1. **이 제품에서 한 상품의 동시 전이는 드물다.** 드문 충돌에 늘 락 대기 비용을 치르는 비관적은 정상 경로(경합 없음)에서도 손해다. 낙관적은 경합이 없으면 비용이 0이다.
2. **전이 규칙은 엔티티(`Product`)에 둔다(`ADR-0006`).** 두 방식 모두 `product.reserve()` 를 쓰므로 만족(조건부 UPDATE 는 이 이유로 애초에 제외).
3. 측정에서도 낙관적이 빨랐다.

→ **낙관적 락(`@Version`)을 채택한다.** 유일한 대가인 "실패 유형 두 가지" 는 예외 핸들러에서
`OptimisticLockingFailureException` 을 409 로 매핑해 흡수한다(예약 유스케이스 `T-21` 에서).

## 재현

```
AWS_ACCESS_KEY=test AWS_SECRET_KEY=test ./gradlew test --tests '*ProductReservationConcurrencyTest'
# 테스트 stdout(build/test-results) 의 [EXP-01] 줄에 방식별 성공/실패/소요가 찍힌다
```
