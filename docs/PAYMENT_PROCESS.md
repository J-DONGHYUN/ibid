# 결제 프로세스 (현재 구현 상태)

이 문서는 현재 코드베이스에 구현된 결제 프로세스를 있는 그대로 기록한다. 설계 의도(`FEATURE_SPEC.md`)와 실제 구현이 어긋난 부분은 [6. 알려진 이슈](#6-알려진-이슈)에 명시한다.

## 1. 관련 엔드포인트

| 유스케이스 | 메서드/경로 | 컨트롤러 | 설명 |
| --- | --- | --- | --- |
| 즉시구매(주문 생성) | `POST /api/orders` | `OrderController.purchase` | `Order` 생성(`CREATED`), 금액 반환 |
| 결제 준비 | `POST /api/payments` | `PaymentController.create` | 주문 금액으로 `Payment`(`READY`) 생성 |
| 결제 승인 | `POST /api/payments/{paymentId}/confirm` | `PaymentController.confirm` | 검증 → Toss 승인 → 확정 처리 |
| 결제 실패/취소 통지 | `POST /api/payments/{paymentId}/fail` | `PaymentController.fail` | 결제에 연결된 주문을 취소 |

## 2. 도메인 상태

**`Order.status`** (`OrderStatus`)

```
CREATED ──(confirm 성공: paid)──▶ PAID ──▶ ... (배송/검수)
   │
   └──(fail 통지: cancel)──▶ CANCELED   (재고 복원)
```

- `CREATED`에서만 `cancel()` 호출 가능.
- `paid()`/`confirmPaid()`도 `CREATED`에서만 가능 (재진입 방지).

**`Payment.state`** (`State`)

```
READY ──(confirm 성공: confirm(paymentKey))──▶ CONFIRMED
```

- `READY`, `CONFIRMED` 두 값만 존재. **실패/취소를 표현하는 상태가 없다** — `fail()`이 호출돼도 `Payment` 자체는 여전히 `READY`로 남는다 (아래 주문 정합성 체크로 재사용을 막는다: 주문이 `CANCELED`가 되면 이후 같은 `paymentId`로 confirm을 시도해도 `PaymentValidator`가 `ORDER_NOT_PAYABLE`로 막는다).

## 3. 성공 시나리오

```
1. [FE] POST /api/orders {productId, quantity}
       └ OrderService.purchase: 본인거래 체크 → Order 저장(CREATED). 재고는 건드리지 않음.
2. [FE] POST /api/payments {orderId}
       └ PaymentService.create: Payment 저장(READY)
3. [FE] Toss 위젯 렌더링 → widgets.requestPayment(successUrl=/checkout/success, failUrl=/checkout/fail)
4. 사용자가 결제 수단 선택 후 승인 → Toss가 successUrl로 리다이렉트
5. [FE] /checkout/success에서 POST /api/payments/{paymentId}/confirm {orderId, amount, paymentKey}
       └ PaymentService.confirm
            ├ PaymentValidator.validate
            │    - 주문이 CREATED인지
            │    - 결제가 READY인지
            │    - 요청 금액 == Payment.totalAmount
            ├ PaymentTossConfirmHandler.confirm
            │    - Toss 승인 API 호출 (TossPaymentClient.requestConfirm)
            │    - TossPayment 저장. 저장 자체가 실패하면 Toss 취소 API 호출 후 예외 재던짐
            └ PaymentProcessor.success
                 - payment.confirm(paymentKey) → CONFIRMED
                 - order.paid() → PAID
                 - productStockHandler.decreaseStock(productId, quantity) ← 재고는 여기서만 차감된다
```

## 4. 실패/취소 시나리오

**(a) Toss 결제창에서 취소·이탈**

```
1~3은 성공 시나리오와 동일
4. 사용자가 Toss 위젯에서 취소/이탈 → Toss가 failUrl(/checkout/fail?paymentId=...&code=...&message=...)로 리다이렉트
5. [FE] /checkout/fail 마운트 시 POST /api/payments/{paymentId}/fail
       └ PaymentService.fail
            - Payment 조회
            - orderService.cancel(payment.getOrderId())
                 - order.cancel() (CREATED → CANCELED, CREATED가 아니면 ORDER_NOT_CANCELABLE)
                 - product.restoreStock(order.getQuantity())  ← 6-2 참고: 이 시점엔 애초에 차감된 적이 없다
```

**(b) confirm 자체가 실패 (검증 실패 / Toss 승인 실패)**

```
1~4는 (a)와 동일하게 진행되다가, 사용자가 결제는 진행해 successUrl로 넘어온 경우
5. [FE] /checkout/success에서 POST .../confirm 호출 → 예외 발생
       - PaymentValidator 단계 실패 (금액 불일치 등): 아무 상태도 안 바뀜, Order는 CREATED로 유지
       - PaymentTossConfirmHandler 단계 실패: TossPayment 저장 실패 시 Toss 쪽 결제만 자체적으로 취소, Order/Payment는 그대로 CREATED/READY
6. [FE] /checkout/success는 에러 메시지만 표시. fail API를 호출하지 않는다.
       → Order가 CREATED로 남고, 재고는 (아직 차감 전이므로) 별도 조치 불필요하지만 주문 자체는 방치된다.
```

## 5. 재고 처리 요약

| 시점 | 코드 | 비고 |
| --- | --- | --- |
| 주문 생성 (`purchase`) | 없음 | 재고 검증/차감을 하지 않는다 |
| 결제 승인 완료 (`PaymentProcessor.success`) | `productStockHandler.decreaseStock` | 여기서만 차감 |
| 결제 취소 통지 (`PaymentService.fail` → `OrderService.cancel`) | `product.restoreStock` | 차감된 적 없는 재고를 복원 시도 (6-2 참고) |

## 6. 알려진 이슈

1. **재고 검증이 Toss 승인 이후에 일어난다.** `purchase()`가 재고를 확인/차감하지 않기 때문에, 재고보다 많은 동시 주문이 모두 `CREATED`로 생성될 수 있다. 뒤늦게 `PaymentProcessor.success()`에서 재고 부족(`INSUFFICIENT_STOCK`)으로 실패하면, 이미 Toss 승인(외부 결제)은 끝난 뒤라 트랜잭션 롤백만으로는 사용자의 결제를 되돌리지 못한다. (원본 리스크 — Codex adversarial review에서 `needs-attention`으로 지적됨)
2. **`fail()` 경로가 활성화되면서 재고가 부풀어나는 문제.** 위 1번과 맞물려, `cancel()`은 재고가 `CREATED` 상태(=아직 차감 전)에서만 호출 가능한데도 무조건 `restoreStock`을 실행한다. 즉 결제 취소/이탈이 일어날 때마다 실제로 차감된 적 없는 수량만큼 재고가 늘어난다. **1번(재고 차감을 주문 생성 시점으로 이동)을 적용해야 같이 해소된다.**
3. **`/checkout/success`의 confirm 실패는 `fail` API를 호출하지 않는다.** 검증 실패나 Toss 승인 실패로 confirm이 예외를 던지면 주문이 `CREATED`로 방치되고 아무도 취소하지 않는다.
4. **완전 이탈(리다이렉트 자체가 없는 경우) 대응 없음.** 사용자가 Toss 위젯이나 결과 페이지에서 아예 브라우저를 닫아버리면 `failUrl`/`successUrl` 어느 쪽도 호출되지 않아 주문이 `CREATED`로 영구히 남는다. 방치 주문 정리용 스케줄러가 없다 (`FEATURE_SPEC.md` §5에 후순위 과제로 명시됨).
5. **`Payment.State`에 실패 상태가 없다.** `fail()`이 호출돼도 `Payment.state`는 계속 `READY`로 남는다. 현재는 `Order.status`가 `CANCELED`로 바뀌어 재승인을 막아주지만, `Payment`만 조회했을 때는 마치 아직 승인 대기 중인 것처럼 보인다.

## 7. 관련 파일

- `order/application/OrderService.java`, `order/domain/Order.java`
- `payment/application/PaymentService.java`, `PaymentValidator.java`, `PaymentProcessor.java`, `PaymentTossConfirmHandler.java`
- `payment/domain/Payment.java`, `State.java`
- `product/application/ProductStockHandler.java`, `product/domain/Product.java`
- `frontend/src/app/checkout/page.tsx`, `checkout/success/page.tsx`, `checkout/fail/page.tsx`
- `frontend/src/lib/api.ts`