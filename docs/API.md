# API 명세서

현재까지 구현된 REST API를 종합한 문서다. 기능 흐름·상태 전이는 [FEATURE_SPEC.md](./FEATURE_SPEC.md), 진행 상태는 [MVP.md](./MVP.md) 참고.

## 공통

- **Base URL**: `/api`
- **인증**: 로그인 후 발급받은 액세스 토큰을 `Authorization: Bearer <accessToken>` 헤더로 전달. `/api/auth/**` 를 제외한 **모든 엔드포인트는 인증 필요**(인터셉터가 강제). 리프레시 토큰은 쿠키로 전달.
- **에러 응답 형식**:
  ```json
  { "code": "PRODUCT_NOT_FOUND", "message": "상품을 찾을 수 없습니다." }
  ```
- **공통 상태 코드**: `400` 잘못된 입력(`INVALID_INPUT`) / `401` 인증 필요(`UNAUTHORIZED`) / `403` 권한 없음(`ACCESS_DENIED`) / `404` 없음 / `409` 상태 충돌 · DB 제약 위반(`DATA_INTEGRITY_VIOLATION`)
- **DB 제약 위반**: 유니크 제약 등을 어기면 `GlobalExceptionHandler`가 `DataIntegrityViolationException`을 받아 일괄 `409 DATA_INTEGRITY_VIOLATION`으로 응답한다(서비스는 잡지 않는다). JPA 경로에서는 중복·FK·길이 초과가 한 예외로 올라와 원인을 구분할 수 없어 메시지는 중립적이며, 상세 원인은 서버 `warn` 로그로 남는다.

> 담당: 인증·상품·주문·검수 = 본인 / **결제(payment) = 팀원**([#15](https://github.com/J-DONGHYUN/ibid/issues/15), 하단 §5).

---

## 1. 인증 (auth)

| 기능 | Method | URL | 인증 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- |
| 회원가입 | POST | `/api/auth/signup` | X | `{email, password, username}` | `201 {userId}` |
| 로그인 | POST | `/api/auth/login` | X | `{email, password}` | `200 {accessToken}` + `Set-Cookie: refreshToken` |
| 로그아웃 | POST | `/api/auth/logout` | 쿠키 | - | `200` + 쿠키 제거 |
| 토큰 재발급 | POST | `/api/auth/refresh` | 쿠키 | - | `200 {accessToken}` (리프레시 회전) |

- 검증: username 4~8자, password 4~12자, 이메일 형식·중복.

---

## 2. 상품 (product)

### 2.1 상품 CRUD

| 기능 | Method | URL | 인증 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- |
| 상품 등록 | POST | `/api/products` | O | `{title, description, price, stock, productCondition}` | `201 {productId}` (생성 시 `PENDING`) |
| 상품 수정 | PATCH | `/api/products/{id}` | 판매자 본인 | `{title, description, price, stock, productCondition}` | `200` |
| 상품 삭제 | DELETE | `/api/products/{id}` | 판매자 본인 | - | `204` (S3 이미지 동기 삭제) |
| 판매 시작 | PATCH | `/api/products/{id}/on-sale` | 판매자 본인 | - | `200` (`PENDING`→`ON_SALE`) |
| 상품 목록 | GET | `/api/products?cursor=` | X | - | `200 {products:[{productId,title,price,stock,status,thumbnailUrl}], nextCursor, hasNext}` |
| 상품 상세 | GET | `/api/products/{id}` | X | - | `200 {productId, sellerId, title, description, price, stock, status, productCondition, imageUrls}` |

- `title` 1~100자, `description` 1~2000자, `price`≥1, `stock`≥1. 목록은 커서 기반(16개, id 내림차순).
- `productCondition` = `NEW` / `LIKE_NEW` / `USED`.
- 수정·삭제는 **거래가 진행되지 않은 상품만** 가능(`PRODUCT_NOT_MODIFIABLE`).
- 오류: `INVALID_PRODUCT_*`(400), `PRODUCT_NOT_FOUND`(404), `PRODUCT_NOT_PENDING`·`PRODUCT_NOT_MODIFIABLE`(409), `ACCESS_DENIED`(403, 판매자 아님).

### 2.2 상품 이미지 (S3 presigned)

파일은 서버를 거치지 않고 **브라우저가 S3에 직접 업로드**한다. 발급 → 업로드 → 확정 3단계.

| 기능 | Method | URL | 인증 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- |
| ① presigned URL 발급 | POST | `/api/products/{id}/images/presign` | 판매자 본인 | `[{fileName, extension}]` | `200 [{uploadUrl, imageUrl}]` |
| ② (브라우저 → S3 직접 `PUT`) | - | - | - | 파일 바이너리 | - |
| ③ 업로드 확정 | POST | `/api/products/{id}/images/confirm` | 판매자 본인 | `{imageUrls:[...]}` | `200` |
| 이미지 개별 삭제 | DELETE | `/api/products/{id}/images` | 판매자 본인 | `{imageUrls:[...]}` | `200` (S3에서도 삭제) |

- 이미지는 `sortOrder` 순으로 정렬되며, 목록의 `thumbnailUrl`은 첫 번째 이미지다.

### 2.3 찜(좋아요)

| 기능 | Method | URL | 인증 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- |
| 찜하기 | POST | `/api/products/{id}/like` | O | - | `200` |
| 찜 취소 | DELETE | `/api/products/{id}/like` | O | - | `204` (멱등 — 찜 안 한 상태여도 성공) |
| 찜 상태 | GET | `/api/products/{id}/like` | O | - | `200 {count, liked}` |
| 관심 목록 | GET | `/api/products/me/likes` | O | - | `200 [{productId,title,price,stock,status,thumbnailUrl}]` |

- 한 사용자는 같은 상품을 **중복 찜할 수 없다** — `unique(user_id, product_id)`로 DB가 강제.
- 동시 요청으로 제약을 위반하면 `409 DATA_INTEGRITY_VIOLATION`. 프론트는 이 경우 찜 상태를 재조회해 화면을 맞춘다.
- 관심 목록은 **찜한 최신순**이며 삭제된 상품은 제외된다.
- 찜수는 `COUNT` + `idx_product_likes_product` 인덱스로 조회한다(**Redis 캐시 미사용** — 근거는 `FEATURE_SPEC.md` §2.3).
- 오류: `PRODUCT_NOT_FOUND`(404, 없는 상품 찜), `DATA_INTEGRITY_VIOLATION`(409).

---

## 3. 주문 (order)

| 기능 | Method | URL | 인증 | 요청 | 응답 |
| --- | --- | --- | --- | --- | --- |
| 상품 구매 | POST | `/api/orders` | O | `{productId, quantity}` | `201 {orderId}` (주문 `CREATED`, 재고 차감) |
| 판매자 발송 | POST | `/api/orders/{id}/ship` | 판매자 본인 | - | `200` (`PAID`→`SHIPPED_TO_INSPECTOR`) |
| 내 거래 목록 | GET | `/api/orders?role=buyer\|seller` | O | - | `200 {orders:[{orderId, productId, productTitle, quantity, totalPrice, status}]}` |
| 내 거래 상세 | GET | `/api/orders/{id}` | 당사자 | - | `200 {orderId, productId, productTitle, buyerId, sellerId, quantity, totalPrice, status}` |

- 본인 상품 구매 불가(`SELF_TRADE_NOT_ALLOWED`), 재고 비관 락. `role` 값 오류 시 `INVALID_INPUT`(400).
- 목록의 `productTitle`은 주문의 `productId`로 상품을 배치 조회(`findAllById`)해 채운다(ID 참조 구조).
- 오류: `PRODUCT_NOT_FOUND`/`ORDER_NOT_FOUND`(404), `INSUFFICIENT_STOCK`/`SOLD_OUT`/`ORDER_NOT_SHIPPABLE`(409), `ACCESS_DENIED`(403, 당사자/판매자 아님).

> `confirmPaid`(`CREATED`→`PAID`)·`cancel`(→`CANCELED`)은 **결제 모듈이 호출하는 유스케이스**로, 주문 자체 HTTP 엔드포인트가 아니다(§5).

---

## 4. 검수 (inspection)

검수업체(운영자)가 호출. 상태 흐름은 Order가 소유하고, Inspection은 판정 결과를 기록한다.

| 기능 | Method | URL | 인증 | 요청 | 응답 / 효과 |
| --- | --- | --- | --- | --- | --- |
| 검수 수령 | POST | `/api/inspections/{orderId}/receive` | O | - | `200` (`SHIPPED_TO_INSPECTOR`→`UNDER_INSPECTION`) |
| 검수 통과 | POST | `/api/inspections/{orderId}/pass` | O | `{memo}` | `200` (`→COMPLETED` + 통과 기록 + `InspectionPassed` 이벤트) |
| 검수 불합격 | POST | `/api/inspections/{orderId}/fail` | O | `{memo}` | `200` (`→REFUNDED` + **재고 복원** + 불합격 기록 + `InspectionFailed` 이벤트) |

- 통과 시 판매자 정산, 불합격 시 구매자 환불은 **이벤트를 결제가 구독**해 처리(현재 구독자 미구현).
- 오류: `ORDER_NOT_FOUND`(404), `ORDER_NOT_INSPECTABLE`/`ORDER_NOT_JUDGEABLE`(409).
- ⚠️ **운영자(ROLE_ADMIN) 권한 제한은 MVP3 예정** — 현재는 로그인 사용자면 호출 가능. 배포 전 필수.

---

## 5. 결제 (payment) — 팀원 담당 / 예정

[#15](https://github.com/J-DONGHYUN/ibid/issues/15) 기준. Toss 연동, 방향 `payment → order`. **구현·계약 확정 전이라 초안**.

| 기능 | Method | URL | 인증 | 요청 | 응답 / 효과 |
| --- | --- | --- | --- | --- | --- |
| 체크아웃 | POST | `/api/payments/checkout` | O | `{productId, quantity}` | `{paymentId, amount, tossOrderId, orderName}` (`orderService.purchase` 호출 → Order `CREATED` + Payment `READY`) |
| 결제 승인 | POST | `/api/payments/{paymentId}/confirm` | O | `{orderId, amount, paymentKey}` | Toss 승인 → Payment `CONFIRMED` → `orderService.confirmPaid`(Order `PAID`) |

- 결제 실패/이탈 시 `orderService.cancel`(재고 복원 + Order `CANCELED`).
- 검수 통과/불합격 이벤트 구독 → 정산/환불(계약 = 이슈 A1/A2 합의 예정).

---

## 6. 주문 상태 흐름 (참고)

```
CREATED ─(결제확정)→ PAID ─(발송)→ SHIPPED_TO_INSPECTOR ─(수령)→ UNDER_INSPECTION ─(통과)→ COMPLETED
   │                                                                              └(불합격)→ REFUNDED
   └(결제 실패/이탈)→ CANCELED
```

각 전이의 상세·이벤트는 [FEATURE_SPEC.md §3](./FEATURE_SPEC.md) 참고.
