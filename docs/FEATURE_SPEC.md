# 기능 명세서

이커머스(중고 거래 + 검수 에스크로) 서비스의 **기능 정의와 전체 플로우**를 다룬다. 팀 전체가 같은 그림을 공유하기 위한 단일 기준 문서다.

> **범위 구분**: 이 문서 = *무엇을 만드는가(기능·플로우·설계)*. 작업 목록·진행 상태·우선순위는 **[MVP.md](./MVP.md)** 에서만 다룬다.

- 대상 코드: `project.kjhjdh.ibid` (Spring Boot 3.5, Java 21, JPA + Redis)
- 담당 구분: **주문 flow = 본인**, **결제(외부 PG/Toss) = 팀원**. 결제-주문 연동 방향은 [#15](https://github.com/J-DONGHYUN/ibid/issues/15)에서 확정 → [§3.1 담당 경계](#31-담당-경계)

---

## 1. 액터 & 용어

| 용어 | 정의 |
| --- | --- |
| 사용자(User) | 가입한 계정. **한 계정이 판매자·구매자 역할을 모두 수행**한다. 별도 판매자/구매자 회원 구분 없음 |
| 판매자(Seller) | 특정 상품을 등록한 사용자. 상품 기준의 역할 이름일 뿐 별도 계정 종류가 아님 |
| 구매자(Buyer) | 특정 상품을 구매한 사용자. 본인이 판매한 상품은 구매할 수 없음 |
| 검수업체(Inspector) | 판매자가 보낸 상품을 검수해 거래 완결/환불을 결정하는 **플랫폼 운영 주체**. 일반 사용자 계정이 아니라 운영자(관리자) 권한으로 동작 |
| 결제(Payment) | 대금의 실제 이동(승인/취소/환불)을 책임지는 관심사. **외부 PG(Toss) 연동**. **팀원 담당**이며 **order를 호출**한다 |
| 에스크로(Escrow) | 결제 완료된 대금을 검수 완료 전까지 플랫폼이 묶어두는 개념. 주문이 `PAID`~`UNDER_INSPECTION` 상태인 동안 대금이 확보돼 있는 상태로 표현 |

---

## 2. 도메인별 기능

### 2.1 인증 (auth)

JWT 액세스 토큰 + 리프레시 토큰(Redis 저장, 쿠키 전달) 기반 인증.

| 기능 | Method | URL | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| 회원가입 | POST | `/api/auth/signup` | X | email/password/username 검증 후 계정 생성 |
| 로그인 | POST | `/api/auth/login` | X | 액세스 토큰(body) + 리프레시 토큰(쿠키) 발급 |
| 로그아웃 | POST | `/api/auth/logout` | 쿠키 | 리프레시 토큰 폐기 |
| 토큰 재발급 | POST | `/api/auth/refresh` | 쿠키 | 리프레시 토큰 회전(rotation) 후 재발급 |

**규칙**
- username 4~8자, password 4~12자, 이메일 형식 검증, 이메일 중복 가입 불가
- 리프레시 토큰은 재발급 시 회전되며, 검증 실패 시 쿠키를 제거하고 401 응답

### 2.2 사용자 (user)

| 필드 | 설명 |
| --- | --- |
| `id` | PK |
| `email` | 값 객체 `Email`, 유일 |
| `password` | 인코딩 저장(BCrypt) |
| `username` | 표시 이름 |

> 대금은 외부 PG로 처리하므로 **User에 잔액(balance) 개념을 두지 않는다.**

### 2.3 상품 (product)

| 필드 | 설명 |
| --- | --- |
| `sellerId` | 등록한 사용자 id |
| `title` | 1~100자 |
| `description` | 1~2000자 |
| `price` | 1원 이상 |
| `stock` | 1개 이상 |
| `status` | `PENDING` → `ON_SALE` → `SOLD_OUT` |
| `productCondition` | `NEW` / `LIKE_NEW` / `USED` |
| `images` | `ProductImage` 목록(`sortOrder` 순, S3 URL). 이미지 소유는 product 도메인이 갖고 S3 기술만 `common.image` 재사용 |

| 기능 | Method | URL | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| 상품 등록 | POST | `/api/products` | O | 로그인 사용자를 판매자로 등록. 생성 시 `PENDING` |
| 상품 수정 | PATCH | `/api/products/{id}` | 판매자 본인 | 거래 진행 전에만 가능 |
| 상품 삭제 | DELETE | `/api/products/{id}` | 판매자 본인 | S3 이미지 동기 삭제 |
| 상품 판매 시작 | PATCH | `/api/products/{id}/on-sale` | 판매자 본인 | `PENDING` → `ON_SALE` |
| 상품 목록 | GET | `/api/products?cursor=` | X | 커서 기반 무한스크롤(16개 단위, id 내림차순) |
| 상품 상세 | GET | `/api/products/{id}` | X | 단건 조회 |
| 이미지 업로드 | POST | `/api/products/{id}/images/presign` · `/confirm` | 판매자 본인 | presigned URL 발급 → 브라우저가 S3 직접 업로드 → 확정 |

**규칙**
- 생성 시 `PENDING`, 판매 시작 시 `ON_SALE`, 재고 0이 되면 `SOLD_OUT`, 구매는 `ON_SALE`만 가능
- 재고 도메인 메서드: `decreaseStock(quantity)`(구매 시), `restoreStock(quantity)`(주문 취소/환불 시 복원; PENDING 상품은 복원 불가)
- 수정·삭제는 거래가 진행된 상품에 대해 차단(`PRODUCT_NOT_MODIFIABLE`)

#### 찜(좋아요) — DTL-2

`ProductLike`는 상품과 분리된 **독립 애그리거트**다. `userId`·`productId`를 **id로 참조**하며 연관관계 매핑을 두지 않는다(찜은 상품의 생명주기에 종속되지 않는 별개 관심사).

| 필드 | 설명 |
| --- | --- |
| `userId` / `productId` | 찜한 사용자·상품 (id 참조) |
| `createdAt` | 찜한 시각 |

| 기능 | Method | URL | 인증 | 설명 |
| --- | --- | --- | --- | --- |
| 찜하기 | POST | `/api/products/{id}/like` | O | 같은 리소스에 메서드로 동작 구분 |
| 찜 취소 | DELETE | `/api/products/{id}/like` | O | 멱등 |
| 찜 상태 | GET | `/api/products/{id}/like` | O | 찜수 + 내 찜여부 |
| 관심 목록 | GET | `/api/products/me/likes` | O | 찜 최신순 상품 요약(썸네일 포함) |

**규칙**
- 중복 찜은 `unique(user_id, product_id)`로 **DB가 강제**한다. 애플리케이션의 `exists` 검사는 빠른 경로일 뿐이고, 검사와 저장 사이의 틈 때문에 동시 요청 시 제약 위반이 날 수 있다 → `409`로 응답(아래 설계 결정).
- 상품 삭제 시 **고아 찜 정리는 미구현**. 관심 목록 조회에서 사라진 상품을 필터링한다.

**설계 결정 (DTL-2)**

| 관심사 | 선택 | 근거 |
| --- | --- | --- |
| 찜수 저장 | **DB `COUNT` + 인덱스** (Redis 캐시 미도입) | 현재 트래픽에서 조기 최적화. 부하 발생 시 재검토 |
| 찜수 조회 인덱스 | `idx_product_likes_product(product_id)` **별도 추가** | 유니크 제약 인덱스는 `(user_id, product_id)` 순이라 leftmost prefix 규칙상 `product_id` 단독 조회에 쓸 수 없다. 없으면 상세 진입마다 풀스캔(10,000건 기준 `EXPLAIN` 9943행 → 20행) |
| 찜 등록 메서드 | **POST** (PUT 아님) | 멱등 계약이 필요 없다 — 중복은 unique 제약이 막는다 |
| 동시 찜 충돌 처리 | **전역 예외 핸들러에서 409** (서비스에서 catch 안 함) | 서비스는 `throw`만 한다는 기존 컨벤션 유지. 앞으로 추가될 다른 유니크 제약도 한 곳에서 커버 |
| 프론트 중복 요청 | in-flight 가드 + 409 시 상태 재조회 | 낙관적 업데이트가 서버와 어긋난 채 남는 것을 방지 |

### 2.4 주문 (order)

주문은 **결제(payment) checkout 시점에 생성**되며, 이후 발송·검수 라이프사이클을 상태 기계로 관리한다.

| 필드 | 설명 |
| --- | --- |
| `productId` / `buyerId` / `sellerId` | 거래 당사자 |
| `quantity` | 구매 수량(1 이상) |
| `totalPrice` | `unitPrice * quantity`. **대금의 진실 원천**(클라 입력 아님) |
| `status` | 주문 상태 → [§3.4 주문 상태 기계](#34-주문-상태-기계) |

> 주문은 `paymentId`를 갖지 않는다. Order↔Payment 연결은 **결제 쪽이 `Payment.orderId`로 참조**한다(order는 payment를 모른다).

**규칙**
- 본인이 등록한 상품은 구매 불가(`SELF_TRADE_NOT_ALLOWED`)
- 재고 차감은 `findByIdForUpdate`(비관적 락)로 동시성 제어 → 초과 판매 방지
- 재고가 0이 되면 상품 `SOLD_OUT`

### 2.5 검수 (inspection)

검수의 **상태 흐름은 Order가 소유**(`SHIPPED_TO_INSPECTOR → UNDER_INSPECTION → COMPLETED/REFUNDED`)하고, `Inspection`은 **판정 결과를 남기는 기록**이다. 검수의 생애주기를 Order 상태기계와 이중으로 두면 동기화 비용이 크므로, 검수는 Order의 한 단계로 두고 판정 근거(누가·왜)만 별도로 남긴다.

- 검수업체(운영자)가 통과/불합격을 판정하는 순간 `Inspection`이 생성되고, 판정 결과를 **도메인 이벤트로 발행**한다(정산/환불은 결제가 구독).

| 필드 | 설명 |
| --- | --- |
| `orderId` | 대상 주문 |
| `inspectorId` | 검수 처리자(운영자) id |
| `result` | `PASSED` / `FAILED` |
| `memo` | 판정 사유/메모 |

### 2.6 결제 연동 (경계)

결제(Toss 연동)는 **팀원 담당**이며, [#15](https://github.com/J-DONGHYUN/ibid/issues/15)에서 **의존성 방향을 payment → order로 확정**했다.

- **구매 오케스트레이션은 결제 모듈이 소유**: 결제의 checkout 파사드가 `OrderService`를 호출해 **Order + Payment를 한 트랜잭션에 원자적으로 생성**한다. 엔드포인트(`/api/payments/checkout`, `/confirm`)도 결제 모듈에 있다.
- **order는 payment/Toss를 import하지 않는다.** 대신 결제가 호출할 유스케이스만 노출한다.

**주문이 결제에 제공하는 유스케이스 (order 소유)**

| 유스케이스 | 호출 시점 | 동작 |
| --- | --- | --- |
| `purchase(buyerId, req)` → `(orderId, totalPrice)` | 결제 checkout | 상품/본인거래 검증 + 재고 차감 + Order 생성(`CREATED`). 금액을 **결과 객체**로 반환(결제가 order repo 직접 접근 안 함) |
| `confirmPaid(orderId)` | 결제 confirm 성공 | 주문 `CREATED` → `PAID` |
| `cancel(orderId)` | 결제 실패/이탈 | 재고 복원 + 주문 → `CANCELED` (보상 처리) |

**검수 정산/환불 (order/inspection → payment, 이벤트)**
- 검수 통과/불합격 시 정산·환불이 필요하지만 **order/inspection은 payment를 직접 호출하지 않는다.**
- inspection이 **도메인 이벤트**(`InspectionPassed` / `InspectionFailed`)를 발행 → **결제가 구독**해 정산/환불을 수행한다. 컴파일 의존성은 여전히 단방향(payment → order) 유지.
- 이벤트 계약(payload, 정산 수단)은 팀원과 합의 필요([§5](#5-미해결논의-필요)).

---

## 3. 거래 flow (주문 + 검수 에스크로)

### 3.1 담당 경계

| 관심사 | 담당 | 책임 |
| --- | --- | --- |
| **주문 (order)** | 본인 | Order 생성/상태 기계, 재고 차감·복원, `purchase`/`confirmPaid`/`cancel` 유스케이스 제공. **payment를 모른다** |
| **검수 (inspection)** | 본인 | 검수 기록/판정, 판정 결과 이벤트 발행 |
| **결제 (payment)** | 팀원 | checkout/confirm 오케스트레이션, Toss 연동, Payment 상태 관리, 정산/환불. **order를 호출/구독** |

**핵심 원칙**
- 의존성 방향은 **payment → order** 한 방향. order/inspection은 payment를 import하지 않는다.
- 대금의 진실 원천은 `Order.getTotalPrice()`.
- 결제 실패/이탈로 인한 재고 누수는 order가 **취소 유스케이스(보상)** 로 복원한다.

### 3.2 핵심 시나리오

```
1. 판매자가 상품 등록 → 판매 시작(ON_SALE)
2. 구매자가 구매 요청  [FE] POST /api/payments/checkout {productId, quantity}
     └ [결제] checkout(@Transactional)
          ├ order.purchase() : 재고 차감 + Order 생성(CREATED), (orderId, totalPrice) 반환
          └ Payment(READY) 저장 + tossOrderId 생성 → 결제창 값 반환
3. [FE] Toss 결제창 → 결제 승인
     └ [결제] confirm : Toss 승인 → Payment CONFIRMED
          └ order.confirmPaid(orderId) : 주문 CREATED → PAID
     └ (실패/이탈) order.cancel(orderId) : 재고 복원 + 주문 CANCELED
4. 판매자가 검수업체로 발송  [주문] ship → SHIPPED_TO_INSPECTOR
5. 검수업체 수령/검수  [검수] receive → UNDER_INSPECTION
     ├ 통과  : 이벤트 InspectionPassed → [결제] 판매자 정산 / [주문] COMPLETED
     └ 불합격 : 이벤트 InspectionFailed → [결제] 구매자 환불 / [주문] REFUNDED
```

### 3.3 추가 시나리오 / 규칙

- **결제 미완/이탈**: `CREATED`로 남은 주문은 `cancel`로 재고 복원 후 `CANCELED`. 방치분은 타임아웃 정리(스케줄러, 후순위) 검토.
- **재고 선점**: 재고는 checkout(주문 생성) 시점에 차감되므로, 결제 이탈 시 반드시 복원해야 재고 누수가 없다.
- **내 거래 목록/상세**: 구매자·판매자가 자기 주문의 진행 상태를 확인.
- **구매자 수령 확정**: 검수 통과 후 즉시 정산으로 단순화. 별도 수령 확정 단계는 두지 않는다.

### 3.4 주문 상태 기계

```
        [checkout] order.purchase → Order 생성
                         │
                         ▼
                     CREATED ──(결제 실패/이탈: cancel)──▶ CANCELED  (재고 복원)
                         │
              (결제 confirm 성공: confirmPaid)
                         ▼
                       PAID ──(판매자 발송: ship)──▶ SHIPPED_TO_INSPECTOR
                                                          │
                                              (검수업체 수령: receive)
                                                          ▼
                                                  UNDER_INSPECTION
                                                      │        │
                                            (통과)             (불합격)
                                                      ▼        ▼
                                                COMPLETED    REFUNDED
```

| 상태 | 의미 | 전이 트리거 |
| --- | --- | --- |
| `CREATED` | 주문 생성, 재고 선점, 결제 확정 대기 | 결제 checkout |
| `PAID` | 결제 완료(대금 확보), 판매자 발송 대기 | 결제 confirm 성공 → `confirmPaid` |
| `CANCELED` | 결제 미완/이탈로 취소, 재고 복원됨 | 결제 실패/이탈 → `cancel` |
| `SHIPPED_TO_INSPECTOR` | 판매자→검수업체 발송 완료 | 판매자 `ship` |
| `UNDER_INSPECTION` | 검수업체 수령·검수 중 | 검수 `receive` |
| `COMPLETED` | 검수 통과, 판매자 정산 완료 | 검수 통과 이벤트 |
| `REFUNDED` | 검수 불합격, 구매자 환불 | 검수 불합격 이벤트 |

- 상태별 허용 액션을 서비스에서 강제. 잘못된 전이는 `409 CONFLICT`.
- 각 전이 액션은 **당사자/권한 검증**을 포함(발송=판매자 본인, 검수=운영자).

### 3.5 API 목록 (소유 도메인 표시)

| 기능 | Method | URL | 소유 | 권한 |
| --- | --- | --- | --- | --- |
| 구매(체크아웃) | POST | `/api/payments/checkout` | 결제(팀원) | 로그인 |
| 결제 승인 | POST | `/api/payments/{paymentId}/confirm` | 결제(팀원) | 로그인 |
| 판매자 발송 | POST | `/api/orders/{id}/ship` | 주문(본인) | 판매자 본인 |
| 내 거래 목록 | GET | `/api/orders?role=buyer\|seller` | 주문(본인) | 로그인 |
| 내 거래 상세 | GET | `/api/orders/{id}` | 주문(본인) | 당사자 |
| 검수 수령 | POST | `/api/inspections/{orderId}/receive` | 검수(본인) | 운영자 |
| 검수 통과 | POST | `/api/inspections/{orderId}/pass` | 검수(본인) | 운영자 |
| 검수 불합격 | POST | `/api/inspections/{orderId}/fail` | 검수(본인) | 운영자 |

---

## 4. 도메인 경계 & 검수업체 설계

**검수업체는 계정(회원 타입)이 아니라 `inspection` 도메인 + 운영자(ROLE_ADMIN) 권한으로 모델링한다.**

- 검수업체는 거래 당사자가 아니라 플랫폼 자신. `User`의 한 종류로 넣으면 역할이 뒤섞인다.
- 검수업체의 일 = (1) 상품 검수 판정 (2) 판정에 따른 정산/환불 트리거(이벤트).

**모듈 구조 & 의존성**

```
order 도메인       : Order 상태기계 + 재고 + purchase/confirmPaid/cancel 유스케이스   ← 본인 (payment 모름)
inspection 도메인  : Inspection 기록 + 판정 이벤트 발행                                ← 본인 (payment 모름)
payment 모듈       : checkout/confirm, Toss 연동, Payment 관리, 정산/환불(이벤트 구독)  ← 팀원 (order 호출/구독)
권한               : 검수 액션은 ROLE_ADMIN

의존성:  payment ──▶ order   (동기 호출: purchase/confirmPaid/cancel)
        inspection ──(도메인 이벤트)──▶ payment   (정산/환불; 컴파일 의존성은 단방향 유지)
```

---

## 5. 미해결·논의 필요

- **정산(판매자 지급) 수단**: 실제 PG에서 판매자에게 대금을 지급하는 방식(정산 API/수동/주기)은 미정. 검수 통과 시 `COMPLETED` 처리와 실제 지급을 분리할지 결정 필요.
- **검수 이벤트 계약**: `InspectionPassed`/`InspectionFailed` payload(orderId 등)와 결제 구독 처리, 실패 시 재시도/보상 정책.
- **트랜잭션/일관성**: checkout에서 `order.purchase` + `Payment(READY)` 원자성(팀원 트랜잭션 경계), confirm 실패 시 Payment `CANCELED` + `order.cancel` 연계.
- **재고 누수 방지**: `CREATED` 방치 주문 타임아웃 정리(스케줄러) 도입 여부.
- **동시성/멱등성**: ship/검수/confirm 중복 호출 방지(멱등), 상태 전이 락.

---

## 6. 범위 밖 (현재 다루지 않음)

- 실제 PG 상세(수수료·정산 주기), 정산 원장(Ledger)
- 배송사 연동, 실 송장 추적
- 알림(이메일/푸시), 분쟁/반품 처리
- 검색/카테고리
