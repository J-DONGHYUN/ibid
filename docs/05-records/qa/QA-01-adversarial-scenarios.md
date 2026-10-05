# QA-01 적대적 QA 시나리오 — 문제 발견용

- 관련: 전 도메인 (product · chat · trade · notification). `requirements.md` · `domain-model.md` 불변식
- 작성일: 2026-10-02 · **실행일: 2026-10-03** (`T-40` · 이슈 #143)
- 목적: **정상 경로가 아니라 깨질 자리**를 적대적으로 찔러 실제 결함을 찾는다. 찾은 결함은
  포트폴리오 트러블슈팅(**문제 → 해결 → 개선**)의 근거가 된다. 단위·통합 테스트가 지금까지
  "되는 것"만 증명해 왔으므로, 이 문서는 "안 되어야 하는데 되는 것"을 겨냥한다.

## 실행 요약

| 구분 | 수 | 결과 |
|---|---|---|
| 결함 발견 (실패) | **4** | QA-1.1 · 1.2 → `T-39` — **해결됨(2026-10-05)** / QA-1.3 · 1.4 → `T-42` |
| 설계 공백 (통과했으나 판단 필요) | 1 | QA-1.5 → `T-43` (결정) |
| 가설이 틀림 | 1 | QA-1.7 — 막힐 줄 몰랐는데 엔티티가 막았다 |
| 통과 (견고성 확인) | 11 | QA-1.6 · Tier 2 (2.1~2.5) · Tier 3 (3.1~3.5) |
| **합계** | **17** | 시나리오 단위 (1.1~1.7 · 2.1~2.5 · 3.1~3.5). 테스트 케이스 수와 다르다 |

**한 줄 결론** — 막는 코드가 **서비스마다 손으로** 들어가 있어서, **들어간 곳만 막힌다.**
같은 "삭제된 상품" 조건이 메시지 전송에서는 막히고(`I-09`) 예약 · 거래완료 · 채팅방 열기에서는
샌다. 엔티티의 상태 가드는 `status` 만 알고 `deletedAt` 을 모른다.

실행 환경: Docker · Testcontainers(MySQL 8.4 · Redis · RabbitMQ 3-management) ·
`AWS_ACCESS_KEY=test AWS_SECRET_KEY=test` · 브랜치 `test/143-qa-01-adversarial`

## 쓰는 법 (나중에 QA 를 실행할 때)

1. 이 문서를 `test` 티켓 하나로 연다 (`ticket` 스킬, 백로그에 `T-40` 로 추가). 시나리오를 테스트로 옮긴다
2. 각 시나리오는 **기대(정상) 동작을 단언**하도록 쓴다 — 결함이 있으면 그 테스트가 **실패**하고, 그 실패가 곧 발견이다
3. 실행 환경: Docker 켜짐 · `AWS_ACCESS_KEY=test AWS_SECRET_KEY=test` · Testcontainers(MySQL·Redis·RabbitMQ). `IntegrationTestSupport` 상속
4. 돌린 뒤 **실제 통과/실패**를 아래 「실행 결과」 칸에 채운다. 가설이 틀려도 그대로 적는다 (지어내지 않는다)
5. 실패한 시나리오는 아래 「발견 기록」 양식으로 **문제 → 해결 → 개선** 을 쓰고, 고칠 것은 백로그 티켓으로 만든다

> **결함으로 실패하는 테스트는 지우지 않는다.** `@Disabled("QA-x.y 발견 … T-nn 에서 막고 이 줄을 지운다")`
> 로 고칠 티켓에 걸어 둔다. 단언은 기대(정상) 동작 그대로 남고, 고치는 티켓이 `@Disabled` 를 푼다 —
> 테스트를 고쳐 통과시키지 않으면서 재현 코드가 저장소에 남는다. `@Disabled` 를 붙이기 **전에 먼저 돌려**
> 실제 실패 출력을 근거로 수집한다.

## 호출 레퍼런스 (테스트 작성용)

| 행위 | 호출 |
|---|---|
| 상품 생성 | `productRepository.save(Product.create(sellerId, title, desc, price, ProductCondition.USED, DeviceSpecFixture.sample()))` |
| 상품 등록(서비스) | `productService.register(sellerId, ProductRegisterCommand(...))` |
| 삭제(소프트) | `productService.delete(sellerId, productId)` |
| 채팅방 열기 | `chatRoomService.open(productId, buyerId)` |
| 메시지 | `chatMessageService.send(new SendMessageCommand(roomId, senderId, clientMsgId, content))` |
| 예약 · 해제 | `reservationService.reserve(productId, sellerId, buyerId)` · `cancelReservation(productId, sellerId)` |
| 거래완료 | `completionService.complete(productId, sellerId, buyerId)` |
| 목록 | `productService.getProducts(cursor, includeSold)` |
| 내역 | `tradeHistoryService.getSales(sellerId, status, cursor)` · `getPurchases(buyerId, cursor)` |
| 알림 | `notificationService.getNotifications(userId, cursor)` · `markRead(notificationId, userId)` |
| 실제 HTTP | `WebIntegrationTestSupport` 상속 → `restTemplate.exchange(path, method, HttpEntity.EMPTY, String.class)` |

---

## Tier 1 — 교차 도메인 불변식 갭 (의심 높음, 포트폴리오 1순위)

> 핵심 가설: **삭제는 `status` 가 아니라 `deletedAt` 플래그**다. 엔티티의 상태 전이 가드(`reserve` 는 ON_SALE,
> `complete` 는 not SOLD)는 `status` 만 보므로 **삭제된 상품을 못 막는다.** 한편 삭제 차단이 서비스마다
> 제각각 들어가(채팅 전송엔 있고 `I-09`, 예약·거래완료·채팅방 열기엔 없음) **비대칭**이 생긴다.
>
> **실행 결과 — 가설이 맞았다.** 네 시나리오(1.1~1.4) 모두 실패했고, 대조군(1.6)은 모두 막혔다.

### QA-1.1 삭제된 상품을 예약할 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 그 상품에 구매자 채팅방 열기 → `productService.delete`
- 행위: `reservationService.reserve(productId, seller, buyer)`
- 기대(정상): 예약 거부 (삭제된 상품은 거래 대상이 아님)
- 의심 근거: `ReservationService.findOwnedProduct` 가 `isDeleted()` 를 안 본다. 삭제돼도 `status==ON_SALE` 이라 `Product.reserve` 가 통과
- 가설: **실패(=버그).** 예약이 성공해 버린다. 이미 백로그 `T-39` 로 식별됨
- **실행 결과: 실패 — 가설대로 버그.** `DeletedProductTradeTest.reserve_deletedProduct` ·
  단언 출력 `Expecting code to raise a throwable.` (거부되지 않고 예약이 성공했다).
  → 「발견 기록」 `[QA-1.1·1.2]`. `@Disabled` 로 `T-39` 에 연결

### QA-1.2 삭제된 상품을 거래완료할 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 구매자 채팅방 → `delete`
- 행위: `completionService.complete(productId, seller, buyer)`
- 기대(정상): 거래완료 거부
- 의심 근거: `CompletionService` 가 `isDeleted()` 를 안 봄. `status != SOLD` 라 `Product.complete` 통과
- 가설: **실패(=버그).** `T-39` 범위
- **실행 결과: 실패 — 가설대로 버그.** `DeletedProductTradeTest.complete_deletedProduct` ·
  같은 단언 출력. 탐침으로 삭제 상품에 `reserve` → `complete` 를 이어 걸면 최종 상태가
  `status=SOLD deletedAt=true reservedBuyerId=null soldBuyerId=20 version=3` 이었다 —
  **숨긴 상품이 전이표를 끝까지 통과한다.** → 「발견 기록」 `[QA-1.1·1.2]`

### QA-1.3 삭제된 상품에 새 채팅방을 열 수 있다 (막혀야 한다)
- 설정: 상품 등록 → `delete` (채팅방 없음)
- 행위: `chatRoomService.open(productId, 새 구매자)`
- 기대(정상): 거부 (`requirements.md` CH-01 — "삭제된 상품에는 새 채팅방을 열 수 없다")
- 의심 근거: `ChatRoomService.open` 은 `PRODUCT_NOT_FOUND` 만 확인, `isDeleted()` 미확인
- 가설: **실패(=버그).** CH-01 요구사항 미구현 갭 (상태 `신규`)
- **실행 결과: 실패 — 가설대로 버그.** `ChatRoomOpenRestrictionTest.open_deletedProduct` ·
  단언 출력 `Expecting code to raise a throwable.` → 「발견 기록」 `[QA-1.3·1.4]`. `T-42` 신설

### QA-1.4 거래완료(SOLD)된 상품에 새 채팅방을 열 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 구매자 채팅방 → 거래완료 → **다른** 구매자가 채팅방 열기
- 행위: `chatRoomService.open(productId, 다른 구매자)`
- 기대(정상): 거부 (CH-01 — "거래완료 상품에는 새 채팅방을 열 수 없다")
- 의심 근거: `open` 이 상품 `status` 를 안 봄
- 가설: **실패(=버그).** CH-01 갭
- **실행 결과: 실패 — 가설대로 버그.** `ChatRoomOpenRestrictionTest.open_soldProduct` · 같은 단언 출력.
  **대조군도 함께 세웠다** — 거래완료 **전에** 열려 있던 거래 상대의 방은 거래완료 뒤에도 같은 방으로
  돌아온다(`open_existingRoomAfterSold`, 통과). 막아야 하는 것은 **새 방**이고, 인수 조율 대화는
  끊어서는 안 된다. `T-42` 가 이 선을 넘지 않는지 재는 기준이다

### QA-1.5 삭제된 상품을 찜할 수 있다 (판단 필요)
- 설정: 상품 등록 → `delete`
- 행위: `productLikeService.like(user, productId)`
- 기대(불확실): `like` 는 `existsById` 만 본다. 소프트 삭제라 행이 남아 찜이 된다. 관심목록(PD-08)에선 걸러지므로 눈에는 안 띔
- 의심 근거: 삭제 상품을 찜하는 게 맞나? 요구사항에 답 없음
- 가설: **통과하지만 의미상 이상.** 결함이라기보다 설계 공백 — 발견되면 묻는다
- **실행 결과: 통과 — 가설대로 설계 공백.** `DeletedProductLikeTest` 두 테스트 모두 통과.
  찜은 되고 `countLikes` 에도 센다. 관심 목록에서는 걸러진다 — **읽히지 않는 찜 행이 쌓인다.**
  `PD-07` 에 삭제 상품 언급이 없어 기대 동작을 단정하지 않고 **현재 동작을 단언으로 고정**했다.
  막을지는 사람이 정한다 → `T-43` (결정)

### QA-1.6 (대조군) SOLD 상품 예약은 막힌다 / 삭제 상품 메시지는 막힌다
- 행위 A: 거래완료 상품에 `reserve` → 기대 거부(`PRODUCT_NOT_ON_SALE`, 엔티티 가드). **통과 예상**
- 행위 B: 삭제 상품 채팅방에 `send` → 기대 거부(`PRODUCT_DELETED`, I-09). **통과 예상**
- 의미: A·B 는 막히는데 QA-1.1~1.4 는 안 막힌다 — **"어디서 막을지(엔티티 상태 가드 vs 서비스 deletedAt 검사)가 일관되지 않다"** 는 교훈. 포트폴리오 「개선」의 핵심 논지
- **실행 결과: A · B 모두 통과 — 가설대로.** A 는 `DeletedProductTradeTest.reserve_soldProduct`
  (`PRODUCT_NOT_ON_SALE`), B 는 `MessageSendRestrictionTest.send_deletedProduct`(`PRODUCT_DELETED`).
  **대조군이 성립해서 「개선」 논지가 추측이 아니라 관찰이 됐다** — 같은 조건이 한 서비스에서는
  막히고 다른 서비스에서는 샌다

### QA-1.7 내 상품에 채팅방을 열 수 있다 (막혀야 한다) — 실행 중 신설
- 설정: 상품 등록 (삭제 · 거래완료 아님)
- 행위: `chatRoomService.open(productId, 판매자 자신)`
- 기대(정상): 거부 (CH-01 — "내 상품에는 채팅방을 열 수 없다")
- 의심 근거: `CH-01` 의 셋째 갈래다. `ChatRoomService.open` 에 판매자 == 구매자 검사가 없어 보였다
- 가설: **실패(=버그).** QA-1.3 · 1.4 와 같은 CH-01 갭일 것
- **실행 결과: 통과 — 가설이 틀렸다.** `ChatRoomOpenRestrictionTest.open_ownProduct` 통과.
  막는 주체가 서비스가 아니라 **`ChatRoom` 엔티티 생성자**였다
  (`ChatRoom.java:50` — `sellerId.equals(buyerId)` → `CANNOT_OPEN_CHAT_ON_OWN_PRODUCT`).
  **이 "틀림"이 오히려 진단을 좁혔다** — CH-01 의 세 갈래 중 **엔티티가 자기 필드로 아는 것만 막히고,
  상품의 `status` · `deletedAt` 을 봐야 하는 둘은 샌다.** 갭의 경계가 "요구사항을 안 지켰다" 가 아니라
  **"판정에 필요한 정보를 가진 자리에만 규칙이 있다"** 는 것

---

## Tier 2 — 견고성 재확인 (동시성·멱등. 통과 예상, "탄탄함" 근거)

> **새로 쓰지 않았다.** 다섯 시나리오가 이미 기존 테스트로 그대로 덮여 있어 중복 대신 재실행해 기록한다.
> 다섯 모두 통과 (2026-10-03).

### QA-2.1 동시 예약 — 한 명만
- 설정: 판매중 상품 + 채팅 상대 N명(각자 채팅방)
- 행위: N 스레드가 서로 다른 상대로 동시에 `reserve`
- 기대: 정확히 1건 성공, 최종 RESERVED, 예약 상대 1명 (`I-12` · 낙관적 락)
- 가설: **통과** (`ReservationConcurrencyTest` 가 이미 덮음 — QA 는 재확인)
- **실행 결과: 통과.** `ReservationConcurrencyTest` (20 스레드, 0.114s). 성공 1 · 실패 19 ·
  최종 `RESERVED` 이고 예약 상대가 그 20명 중 한 명 — 성공 수만이 아니라 최종 상태까지 단언한다

### QA-2.2 예약 해제 vs 거래완료 동시
- 행위: 예약중 상품에 `cancelReservation` 과 `complete` 동시
- 기대: 최종 상태가 일관(SOLD+거래상대 / ON_SALE+상대없음 중 하나), 찢어지지 않음 (`I-12`)
- 가설: **통과** (`CompletionConcurrencyTest` 재확인)
- **실행 결과: 통과.** `CompletionConcurrencyTest` (0.089s). 최종 상태가 두 전이 중 하나의 일관된
  결과이고 중간 상태가 남지 않는다

### QA-2.3 같은 clientMessageId 동시 전송 — 하나만 저장
- 행위: 같은 (방, clientMessageId) 로 N 스레드 동시 `send`
- 기대: 메시지 1건 (`I-07` DB 유니크 + 멱등 재조회)
- 가설: **통과** (`ChatMessageConcurrencyTest` 재확인)
- **실행 결과: 통과.** `ChatMessageConcurrencyTest` (0.608s)

### QA-2.4 같은 (상품, 구매자) 동시 채팅방 열기 — 하나만
- 행위: 같은 쌍으로 N 스레드 동시 `open`
- 기대: 방 1개 (`I-06` DB 유니크 + 멱등)
- 가설: **통과** (`ChatRoomConcurrencyTest` 재확인)
- **실행 결과: 통과.** `ChatRoomConcurrencyTest` (0.092s)

### QA-2.5 알림 발행 실패가 원 작업을 막지 않는다 (I-11)
- 행위: `NotificationEventPublisher` 가 던지도록(브로커 장애 가정) 두고 `send` / `complete` 실행
- 기대: 메시지 저장 · 거래완료는 성공. 알림만 유실
- 가설: **통과** (`TradeNotificationResilienceTest`·발행기 삼킴 재확인)
- **실행 결과: 통과.** `TradeNotificationResilienceTest` (0.09s). `AmqpException` 을 던지게 해도
  거래완료가 커밋된다. 실제 브로커를 내린 주입은 `QA-02` Tier A 몫

---

## Tier 3 — 커버리지 · 방어 공백 (실행해서 확인)

### QA-3.1 인증 없이 보호 API 호출 → 401
- 행위: 토큰 없이 `GET /api/notifications` · `POST /api/products/{id}/reservation` 등 로그인 등급 호출
- 기대: 401 (AuthenticationInterceptor)
- 의심 근거: 공개 API(`@PublicApi`)와 보호 API 의 경계가 실제로 지켜지는지 통합으로 안 본 듯
- 가설: **통과 예상** 이나 커버리지 공백 — MockMvc 가 아니라 실제 인터셉터 경로로 확인
- **실행 결과: 통과 (18/18).** `ProtectedEndpointAuthTest` — 보호 API 16개가 토큰 없이 401,
  `@PublicApi` 가 붙은 목록 · 상세 2개는 401 이 아니다. 가설대로 **동작은 멀쩡했고 공백은 커버리지**였다.
  기존 `AuthenticationInterceptorTest` 는 인터셉터의 **판정**만 보고, 그것이 이 경로들에 **등록**됐는지는
  증명하지 못한다 — `WebConfig.addPathPatterns("/**")` 가 끊기면 단위 테스트는 초록인 채 모든 API 가
  열린다. 그 사각지대를 메우려 `WebIntegrationTestSupport`(`RANDOM_PORT`)를 더했다
- **남은 공백**: 경로 목록을 손으로 적었다. 새 컨트롤러가 생기면 추가해야 하고, 빠지면 조용히 안 덮인다 —
  `PublicEndpointRulesTest` 처럼 게이트로 셀 수 있는지는 더 볼 거리

### QA-3.2 남의 자원 조작 → 403
- 행위: 타인 상품 `reserve`/`complete`/`delete`/`update`, 남의 알림 `markRead`, 남의 채팅방 조회
- 기대: 403 (ACCESS_DENIED)
- 가설: **통과 예상** (서비스 단위 테스트에 있음) — 교차 확인
- **실행 결과: 통과 — 기존 테스트로 덮여 있어 새로 쓰지 않았다.** `ACCESS_DENIED` 를 단언하는 테스트가
  15개 파일에 있다 — `ReservationServiceTest` · `CompletionServiceTest` · `ProductServiceTest` ·
  `ChatRoomServiceTest` · `ChatMessageServiceTest` · `NotificationServiceTest` 와 각 컨트롤러 테스트.
  여섯 자원 모두 본인 · 남 조합이 있다

### QA-3.3 입력 검증 경계
- 행위: 가격 0·음수, 제목 101자, 메시지 1001자, 배터리 성능 -1·101, 커서에 음수/거대값
- 기대: 400 (검증) 또는 안전한 빈 결과
- 가설: 대부분 **통과**, 커서 경계값은 실제 확인 필요
- **실행 결과: 통과.** 커서는 `ProductListCursorBoundaryTest` 로 새로 세웠다 — 음수 · 0 ·
  `Long.MAX_VALUE` 모두 예외 없이 빈 목록이거나 첫 페이지다. 커서 없음이 `Long.MAX_VALUE` 로
  치환되는 것과 커서가 자신을 제외하는 것(`idLessThan`)도 고정했다. `chat` · `notification` 의 커서도
  같은 관용구 + `idLessThan` 이라 결론이 같다 — **코드 리딩으로만 확인했고 따로 테스트하지 않았다**.
  메시지 길이는 검증은 있는데 테스트가 없어 1000 허용 · 1001 거부를 `ChatMessageTest` 에 더했다.
  제목 100자 · 가격 1원 · 배터리 0~100 은 `ProductTest` · `DeviceSpecTest` 가 이미 경계로 덮는다

### QA-3.4 거래완료 상품에 메시지 전송 (의도 확인)
- 행위: SOLD 상품의 기존 채팅방에 `send`
- 기대(불확실): I-09 는 삭제만 막고 SOLD 는 안 막음 → 전송됨. 거래 후 인수 조율 대화라 의도된 것으로 보임
- 가설: **통과(의도된 동작).** 결함 아님 — 문서에 "SOLD 방은 대화 가능" 을 명시할지 판단
- **실행 결과: 통과 — 가설대로 의도된 동작.** `MessageSendRestrictionTest.send_soldProduct`.
  `I-09` 가 삭제만 막는 것이 누락이 아니라 선택임을 테스트로 고정했다 (거래 뒤 인수 조율을 끊지 않는다)

### QA-3.5 I-04 교차 상품 — 다른 상품의 채팅 상대를 예약
- 행위: 상품 A 에만 채팅방이 있는 구매자를 상품 B 에 예약 시도
- 기대: 거부 (`isChatPartner` 는 (상품, 구매자)별)
- 가설: **통과 예상** (product 별 조회) — 교차 확인
- **실행 결과: 통과.** `DeletedProductTradeTest.reserve_chatPartnerOfAnotherProduct` ·
  `NOT_CHAT_PARTNER` 로 거부되고 상품 B 의 상태도 그대로다

---

## 발견 기록

### [QA-1.1·1.2] 삭제된(숨긴) 상품이 예약 · 거래완료된다

**문제** — 판매자가 상품을 삭제한 뒤에도 그 상품을 예약하고 거래완료할 수 있다.
재현: 상품 등록 → 구매자 채팅방 열기 → `productService.delete(seller, productId)` →
`reservationService.reserve(productId, seller, buyer)`.
기대는 거부인데 `assertThatThrownBy` 가 `Expecting code to raise a throwable.` 로 깨졌다 —
예외 없이 성공했다. 탐침으로 `reserve` → `complete` 를 이어 걸고 최종 상태를 재면

```
status=SOLD deletedAt=true reservedBuyerId=null soldBuyerId=20 version=3
```

**목록 · 상세에서 사라진 상품이 `ON_SALE → RESERVED → SOLD` 를 끝까지 통과한다.**
상세는 404 인데 거래는 성립하고, `ADR-0011` 대로 구매 · 판매 내역에는 남는다 —
구매자에게는 **열 수 없는 상품의 구매 기록**이 생긴다.

**원인** — 삭제 판정이 조회 경로에만 들어가 있다.

- `ProductService.findOwnedProduct`(`ProductService.java:112`)는 `.filter(p -> !p.isDeleted())` 가 있다
- **같은 이름**의 `ReservationService.findOwnedProduct`(`ReservationService.java:45`)에는 없다
- `CompletionService.complete`(`CompletionService.java:26`)도 `findById` 뒤 소유자만 본다

엔티티의 전이 가드는 막을 수 없다 — `Product.reserve`(`Product.java:102`)는 `status != ON_SALE` 만,
`Product.complete`(`Product.java:119`)는 `status == SOLD` 만 본다. **`deletedAt` 은 `status` 와 다른
축인데 전이 규칙은 `status` 만 안다.** 소프트 삭제(`ADR-0011`)를 고를 때 "숨긴다"는 조회 요구로만
번역되고, "거래 대상에서 뺀다"는 전이 요구로는 번역되지 않았다.

**해결** — `T-39`. 이 티켓은 재현과 기록까지만 했고, 고치는 길은 결정 → 도구 → 수정 순으로 열렸다.
처음에는 고칠 자리의 선택지를 ① `trade` 의 두 서비스에 `isDeleted()` 검사 추가 ② `Product` 에 "거래 가능" 판정을 두고 전이 메서드가 `deletedAt` 까지 보게 함, 둘로 적고 `T-39` 에서 정한다고 했다.
**그 뒤 `T-43` 에서 둘 다 아닌 방식으로 정했다(2026-10-05, `ADR-0013`)** — 쓰기 경로가 상품을 얻는 조회를 삭제를 제외하는 하나(`findActiveById`)로 모으고, 날것의 `findById` 호출을 게이트로 센다. ①은 이번 결함의 원인과 같은 방식이라 재발하고, ②는 상태 전이만 닿고 채팅방 열기 · 찜은 못 닿기 때문이었다.
도구는 `T-52` 가 만들었다(전용 조회 + 게이트 + 처음 기준선 4줄). `T-39` 는 `ReservationService` · `CompletionService` 가 그 조회로 상품을 얻게 했다. 삭제된 상품은 조회가 돌려주지 않아 404 `PRODUCT_NOT_FOUND` 가 된다.
예약 해제는 예약과 같은 `findOwnedProduct` 를 쓰므로 한 번에 막혔다. 서비스마다 `isDeleted()` 를 흩뿌리지 않았다.
`@Disabled("QA-1.1 발견 … T-39")` 를 지우는 것이 수정 검증이었다. 처음에 `BusinessException` 까지만 걸어 두었던 단언은 404 · 상태 불변으로 조였다.

**개선 결과** — 같은 테스트(`DeletedProductTradeTest`)로 고치기 전후를 쟀다.

| 항목 | 고치기 전 | 고친 후 |
|---|---|---|
| 삭제된 상품에 예약 → 해제 → 거래완료를 차례로 시도해 **성공한 연산** | **3 / 3** | **0 / 3** |
| 시도 뒤 최종 상태 | `status=SOLD deleted=true reservedBuyerId=null soldBuyerId=20` | `status=ON_SALE deleted=true reservedBuyerId=null soldBuyerId=null` (처음 그대로) |
| 판매자가 아닌 사람이 삭제된 상품을 예약 · 거래완료 | **403** "접근 권한이 없습니다" (삭제된 상품이 존재한다는 것을 알려 줌) | **404** |
| 삭제되지 않은 상품의 예약 · 해제 · 거래완료 (과차단 확인) | 정상 | 정상 — 서비스 · 동시성 · 컨트롤러 52개 통과 (동시 예약 20명 중 1명 포함) |
| 상품을 날것의 id 조회로 읽는 쓰기 경로 (동결 기준선) | 4곳 | **2곳** (`T-42` · `T-53` 몫) |

한계: **예약중 상품을 삭제하면 `RESERVED` 로 동결되고 예약 해제도 막힌다** — `ADR-0013` 이 바꾸지 않기로 한 부분이라 테스트(`cancelReservation_deletedProduct`)로 동작만 고정했다.
삭제와 예약이 **동시에** 들어오는 경우는 재지 않았다. 낙관적 락(`@Version`)이 늦게 커밋하는 쪽을 막을 것으로 보이지만 **확인하지 않았다.**
판매 내역에는 삭제한 상품이 그대로 나온다(`ADR-0011` 의도).

**개선** — 이 결함이 드러낸 교훈은 **"어디서 막을지가 일관되지 않다"** 다.
같은 "삭제된 상품" 조건이 메시지 전송에서는 막히고(`ChatMessageService.send` 가 상품을 다시 조회해
`isDeleted()` 를 본다, `I-09`) 예약 · 거래완료 · 채팅방 열기에서는 샜다. 막는 코드가 **서비스마다 손으로**
들어가 있어서 **들어간 곳만 막혔다.** 대조군(QA-1.6)이 통과해서 이 논지가 추측이 아니라 관찰이다.
**처음 적은 재발 방지안은 틀렸다.** "`Product` 의 전이 가드가 `deletedAt` 까지 보는 쪽이 샐 자리가 적다" 고 적었는데,
결정 과정에서 그 방식은 상태 전이만 닿고 채팅방 열기 · 찜은 닿지 않는다는 것이 드러나 쓰기 경로 전용 조회로 바뀌었다.
기록을 지우지 않고 남긴다 — 가설과 결정이 갈린 자리가 이 흐름에서 배운 것이다.
규칙을 한 곳으로 모으는 방향은 맞았고, **어디인지**가 달랐다. 판정에 필요한 정보(`deletedAt`)는 엔티티에 있지만 **강제할 자리**는 상품을 얻는 조회였다.
그리고 그 한 곳을 **게이트로 지킨다** — 날것의 조회가 새로 늘면 CI 가 실패한다(`T-52`). 남은 4줄이 2줄이 됐고 `T-42` · `T-53` 이 0 으로 만든다.

### [QA-1.3·1.4] 삭제 · 거래완료된 상품에 새 채팅방이 열린다

**문제** — `CH-01` 의 검증 기준 "거래완료 · 삭제된 상품에는 새 채팅방을 열 수 없다" 가 지켜지지 않는다.
재현 A: 상품 등록 → `delete` → `chatRoomService.open(productId, 새 구매자)` → 방이 열린다.
재현 B: 상품 등록 → 거래완료 → **거래와 무관한 다른 구매자**가 `open` → 방이 열린다.
둘 다 `Expecting code to raise a throwable.` 로 단언이 깨졌다.
B 는 이미 팔린 물건에 모르는 사람이 말을 걸 수 있다는 뜻이다.

**원인** — `ChatRoomService.open`(`ChatRoomService.java:38`)은 상품을 `findById` 로 찾아
**존재만** 확인한다(`PRODUCT_NOT_FOUND`). `isDeleted()` 도 `status` 도 보지 않는다.
`CH-01` 은 `requirements.md` 에서 상태가 아직 `신규` 다 — 미구현이 정직하게 표시된 요구사항이고,
`T-15`(채팅방 열기)의 참고에 "거래완료 · 삭제 상품 조건은 T-22 · T-13 이후에 함께 검증한다" 로
미뤄 둔 것이 **선행이 끝난 뒤에도 돌아오지 않았다.**

**해결** — 아직 고치지 않았다. `T-42` 를 신설했다.
재현 테스트는 `ChatRoomOpenRestrictionTest` 에 `@Disabled` 로 `T-42` 에 걸려 있다.
**경계를 같이 세워 뒀다** — `open_existingRoomAfterSold`(통과)는 거래완료 **전에** 열린 방이
거래완료 뒤에도 같은 방으로 돌아오는 것을 단언한다. `T-42` 는 **새 방만** 막아야 하고
인수 조율 대화를 끊어서는 안 된다.

**개선** — `T-15` 가 조건을 미룬 것은 옳았다(선행이 없었다). 샌 것은 **미룬 것을 되찾는 장치가 없었다**는
점이다. 티켓 참고의 "이후에 함께 검증한다" 는 사람도 에이전트도 다시 읽지 않는다.
`requirements.md` 의 상태 열이 `신규` 로 남아 이 갭을 가리키고 있었는데, 그 열을 코드와 맞추는 일
자체가 아직 미착수 티켓(`T-38`)이라 신호가 묻혔다. **부분 구현을 추적하는 자리가 티켓 본문의 산문이
아니라 요구사항의 상태 열이어야 한다** — `T-38` 의 값이 문서 정리가 아니라 **갭 추적**이라는 근거다.
QA-1.7 과 함께 보면 더 좁은 교훈이 나온다: `CH-01` 의 세 갈래 중 **`ChatRoom` 엔티티가 자기 필드로
판정할 수 있는 것(판매자 == 구매자)만 막히고, 상품의 `status` · `deletedAt` 을 봐야 하는 둘은 샌다.**

### [QA-1.5] 삭제된 상품을 찜할 수 있다 — 결함이 아니라 설계 공백

**문제** — 삭제된 상품을 찜할 수 있고 `countLikes` 에도 센다. 관심 목록(`PD-08`)에서는 걸러지므로
사용자 눈에는 안 띈다. 즉 **읽히지 않는 찜 행이 쌓인다.** 정상 UI 로는 닿을 수 없다(상세가 404) —
API 로만 가능하다.

**원인** — `ProductLikeService.like`(`ProductLikeService.java:29`)는 `existsById` 만 본다.
소프트 삭제라 행이 남아 `true` 다. `myLikedProducts`(`:67`)에만 `.filter(product -> !product.isDeleted())`
가 있다.

**해결** — 하지 않았다. **요구사항에 답이 없어 단정하지 않았다.** `PD-07` 에는 삭제 상품 언급이 없고,
`PD-06` · `PD-08` 은 "목록 · 상세 · 관심 목록에 안 나온다" 까지만 정한다. 지금 동작을
`DeletedProductLikeTest` 에 단언으로 **고정**해 두었다 — 어느 쪽으로 정하든 바뀌는 자리가 드러난다.
막을지는 사람이 정한다 → `T-43`(결정). **정했다(2026-10-05, `ADR-0013`): 삭제된 상품은 찜할 수 없고 찜 취소는 허용한다** — 구현은 `T-53`.

**개선** — QA-1.1~1.4 와 **같은 뿌리인데 영향이 다르다.** 뿌리는 "소프트 삭제 뒤 어떤 연산이 허용되나"
를 한 번에 정하지 않고 연산마다 따로 처리한 것이다. 상태를 바꾸는 연산(예약 · 거래완료 · 방 열기)은
결함이고, 읽히지 않는 데이터를 남기는 연산(찜)은 공백이다. `ADR-0011` 이 "숨길 조회에만
`deletedAt IS NULL` 을 건다" 고 조회만 정했기 때문에, **쓰기 연산의 기준이 비어 있었다.**
`T-39` · `T-42` 를 고칠 때 연산 하나씩 패치하지 말고 이 기준을 먼저 적는 쪽이 재발을 줄인다.

## 발견 기록 양식 (실패한 시나리오마다)

```
### [QA-x.y] 한 줄 제목

**문제** — 무엇이 어떻게 잘못됐나. 재현 시나리오와 실제 관찰(어떤 단언이 깨졌나).
   근거 로그 · 상태값을 붙인다.

**원인** — 코드의 어느 지점이 왜 이 동작을 내는가 (파일:줄, 설계상 누락).

**해결** — 어떻게 고쳤나 (어느 계층에서 막았나, 에러 코드, 커밋/PR).

**개선** — 이 결함이 드러낸 더 큰 교훈. 재발 방지(게이트·불변식·문서),
   같은 종류의 다른 자리도 함께 고쳤는지.
```

## 수확 (실행 후)

| 시나리오 | 가설 | 실제 | 포트폴리오 가치 |
|---|---|---|---|
| QA-1.1 · 1.2 | 실패(버그) | **실패 — 맞음.** 숨긴 상품이 SOLD 까지 간다 | 높음 (`T-39`, "상태 vs 플래그" 설계 교훈) |
| QA-1.3 · 1.4 | 실패(버그) | **실패 — 맞음.** CH-01 미구현 | 높음 (요구사항-구현 갭, `T-42`) |
| QA-1.5 | 통과하나 이상 | 통과 — 맞음. 설계 공백 | 중 (`T-43` 결정, 「쓰기 기준이 비었다」) |
| QA-1.6 (대조) | 통과 | 통과 — 비대칭 드러냄 | 높음 (「개선」 논지의 축) |
| QA-1.7 (신설) | 실패(버그) | **통과 — 틀림.** 엔티티가 막았다 | 높음 (갭의 경계를 좁혔다) |
| Tier 2 | 통과 | 통과 (5/5) | 중 ("무엇이 견고한가" 근거) |
| Tier 3 | 대체로 통과 | 통과 (401 18/18 · 커서 · 403) | 중 (방어 커버리지 보강) |

**가설 적중 16 / 17** (Tier 1 만 보면 6 / 7). 틀린 하나(QA-1.7)가 진단을 넓히는 대신 좁혔다 —
"요구사항을 안 지켰다" 가 아니라 **"판정에 필요한 정보를 가진 자리에만 규칙이 있다"**.
