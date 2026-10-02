# QA-01 적대적 QA 시나리오 — 문제 발견용

- 관련: 전 도메인 (product · chat · trade · notification). `requirements.md` · `domain-model.md` 불변식
- 작성일: 2026-10-02
- 목적: **정상 경로가 아니라 깨질 자리**를 적대적으로 찔러 실제 결함을 찾는다. 찾은 결함은
  포트폴리오 트러블슈팅(**문제 → 해결 → 개선**)의 근거가 된다. 단위·통합 테스트가 지금까지
  "되는 것"만 증명해 왔으므로, 이 문서는 "안 되어야 하는데 되는 것"을 겨냥한다.

## 쓰는 법 (나중에 QA 를 실행할 때)

1. 이 문서를 `test` 티켓 하나로 연다 (`ticket` 스킬, 백로그에 `T-40` 로 추가). 시나리오를 테스트로 옮긴다
2. 각 시나리오는 **기대(정상) 동작을 단언**하도록 쓴다 — 결함이 있으면 그 테스트가 **실패**하고, 그 실패가 곧 발견이다
3. 실행 환경: Docker 켜짐 · `AWS_ACCESS_KEY=test AWS_SECRET_KEY=test` · Testcontainers(MySQL·Redis·RabbitMQ). `IntegrationTestSupport` 상속
4. 돌린 뒤 **실제 통과/실패**를 아래 「실행 결과」 칸에 채운다. 가설이 틀려도 그대로 적는다 (지어내지 않는다)
5. 실패한 시나리오는 아래 「발견 기록」 양식으로 **문제 → 해결 → 개선** 을 쓰고, 고칠 것은 백로그 티켓으로 만든다

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

---

## Tier 1 — 교차 도메인 불변식 갭 (의심 높음, 포트폴리오 1순위)

> 핵심 가설: **삭제는 `status` 가 아니라 `deletedAt` 플래그**다. 엔티티의 상태 전이 가드(`reserve` 는 ON_SALE,
> `complete` 는 not SOLD)는 `status` 만 보므로 **삭제된 상품을 못 막는다.** 한편 삭제 차단이 서비스마다
> 제각각 들어가(채팅 전송엔 있고 `I-09`, 예약·거래완료·채팅방 열기엔 없음) **비대칭**이 생긴다.

### QA-1.1 삭제된 상품을 예약할 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 그 상품에 구매자 채팅방 열기 → `productService.delete`
- 행위: `reservationService.reserve(productId, seller, buyer)`
- 기대(정상): 예약 거부 (삭제된 상품은 거래 대상이 아님)
- 의심 근거: `ReservationService.findOwnedProduct` 가 `isDeleted()` 를 안 본다. 삭제돼도 `status==ON_SALE` 이라 `Product.reserve` 가 통과
- 가설: **실패(=버그).** 예약이 성공해 버린다. 이미 백로그 `T-39` 로 식별됨
- 실행 결과: _(미실행)_

### QA-1.2 삭제된 상품을 거래완료할 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 구매자 채팅방 → `delete`
- 행위: `completionService.complete(productId, seller, buyer)`
- 기대(정상): 거래완료 거부
- 의심 근거: `CompletionService` 가 `isDeleted()` 를 안 봄. `status != SOLD` 라 `Product.complete` 통과
- 가설: **실패(=버그).** `T-39` 범위
- 실행 결과: _(미실행)_

### QA-1.3 삭제된 상품에 새 채팅방을 열 수 있다 (막혀야 한다)
- 설정: 상품 등록 → `delete` (채팅방 없음)
- 행위: `chatRoomService.open(productId, 새 구매자)`
- 기대(정상): 거부 (`requirements.md` CH-01 — "삭제된 상품에는 새 채팅방을 열 수 없다")
- 의심 근거: `ChatRoomService.open` 은 `PRODUCT_NOT_FOUND` 만 확인, `isDeleted()` 미확인
- 가설: **실패(=버그).** CH-01 요구사항 미구현 갭 (상태 `신규`)
- 실행 결과: _(미실행)_

### QA-1.4 거래완료(SOLD)된 상품에 새 채팅방을 열 수 있다 (막혀야 한다)
- 설정: 상품 등록 → 구매자 채팅방 → 거래완료 → **다른** 구매자가 채팅방 열기
- 행위: `chatRoomService.open(productId, 다른 구매자)`
- 기대(정상): 거부 (CH-01 — "거래완료 상품에는 새 채팅방을 열 수 없다")
- 의심 근거: `open` 이 상품 `status` 를 안 봄
- 가설: **실패(=버그).** CH-01 갭
- 실행 결과: _(미실행)_

### QA-1.5 삭제된 상품을 찜할 수 있다 (판단 필요)
- 설정: 상품 등록 → `delete`
- 행위: `productLikeService.like(user, productId)`
- 기대(불확실): `like` 는 `existsById` 만 본다. 소프트 삭제라 행이 남아 찜이 된다. 관심목록(PD-08)에선 걸러지므로 눈에는 안 띔
- 의심 근거: 삭제 상품을 찜하는 게 맞나? 요구사항에 답 없음
- 가설: **통과하지만 의미상 이상.** 결함이라기보다 설계 공백 — 발견되면 묻는다
- 실행 결과: _(미실행)_

### QA-1.6 (대조군) SOLD 상품 예약은 막힌다 / 삭제 상품 메시지는 막힌다
- 행위 A: 거래완료 상품에 `reserve` → 기대 거부(`PRODUCT_NOT_ON_SALE`, 엔티티 가드). **통과 예상**
- 행위 B: 삭제 상품 채팅방에 `send` → 기대 거부(`PRODUCT_DELETED`, I-09). **통과 예상**
- 의미: A·B 는 막히는데 QA-1.1~1.4 는 안 막힌다 — **"어디서 막을지(엔티티 상태 가드 vs 서비스 deletedAt 검사)가 일관되지 않다"** 는 교훈. 포트폴리오 「개선」의 핵심 논지
- 실행 결과: _(미실행)_

---

## Tier 2 — 견고성 재확인 (동시성·멱등. 통과 예상, "탄탄함" 근거)

### QA-2.1 동시 예약 — 한 명만
- 설정: 판매중 상품 + 채팅 상대 N명(각자 채팅방)
- 행위: N 스레드가 서로 다른 상대로 동시에 `reserve`
- 기대: 정확히 1건 성공, 최종 RESERVED, 예약 상대 1명 (`I-12` · 낙관적 락)
- 가설: **통과** (`ReservationConcurrencyTest` 가 이미 덮음 — QA 는 재확인)
- 실행 결과: _(미실행)_

### QA-2.2 예약 해제 vs 거래완료 동시
- 행위: 예약중 상품에 `cancelReservation` 과 `complete` 동시
- 기대: 최종 상태가 일관(SOLD+거래상대 / ON_SALE+상대없음 중 하나), 찢어지지 않음 (`I-12`)
- 가설: **통과** (`CompletionConcurrencyTest` 재확인)
- 실행 결과: _(미실행)_

### QA-2.3 같은 clientMessageId 동시 전송 — 하나만 저장
- 행위: 같은 (방, clientMessageId) 로 N 스레드 동시 `send`
- 기대: 메시지 1건 (`I-07` DB 유니크 + 멱등 재조회)
- 가설: **통과** (`ChatMessageConcurrencyTest` 재확인)
- 실행 결과: _(미실행)_

### QA-2.4 같은 (상품, 구매자) 동시 채팅방 열기 — 하나만
- 행위: 같은 쌍으로 N 스레드 동시 `open`
- 기대: 방 1개 (`I-06` DB 유니크 + 멱등)
- 가설: **통과** (`ChatRoomConcurrencyTest` 재확인)
- 실행 결과: _(미실행)_

### QA-2.5 알림 발행 실패가 원 작업을 막지 않는다 (I-11)
- 행위: `NotificationEventPublisher` 가 던지도록(브로커 장애 가정) 두고 `send` / `complete` 실행
- 기대: 메시지 저장 · 거래완료는 성공. 알림만 유실
- 가설: **통과** (`TradeNotificationResilienceTest`·발행기 삼킴 재확인)
- 실행 결과: _(미실행)_

---

## Tier 3 — 커버리지 · 방어 공백 (실행해서 확인)

### QA-3.1 인증 없이 보호 API 호출 → 401
- 행위: 토큰 없이 `GET /api/notifications` · `POST /api/products/{id}/reservation` 등 로그인 등급 호출
- 기대: 401 (AuthenticationInterceptor)
- 의심 근거: 공개 API(`@PublicApi`)와 보호 API 의 경계가 실제로 지켜지는지 통합으로 안 본 듯
- 가설: **통과 예상** 이나 커버리지 공백 — MockMvc 가 아니라 실제 인터셉터 경로로 확인
- 실행 결과: _(미실행)_

### QA-3.2 남의 자원 조작 → 403
- 행위: 타인 상품 `reserve`/`complete`/`delete`/`update`, 남의 알림 `markRead`, 남의 채팅방 조회
- 기대: 403 (ACCESS_DENIED)
- 가설: **통과 예상** (서비스 단위 테스트에 있음) — 교차 확인
- 실행 결과: _(미실행)_

### QA-3.3 입력 검증 경계
- 행위: 가격 0·음수, 제목 101자, 메시지 1001자, 배터리 성능 -1·101, 커서에 음수/거대값
- 기대: 400 (검증) 또는 안전한 빈 결과
- 가설: 대부분 **통과**, 커서 경계값은 실제 확인 필요
- 실행 결과: _(미실행)_

### QA-3.4 거래완료 상품에 메시지 전송 (의도 확인)
- 행위: SOLD 상품의 기존 채팅방에 `send`
- 기대(불확실): I-09 는 삭제만 막고 SOLD 는 안 막음 → 전송됨. 거래 후 인수 조율 대화라 의도된 것으로 보임
- 가설: **통과(의도된 동작).** 결함 아님 — 문서에 "SOLD 방은 대화 가능" 을 명시할지 판단
- 실행 결과: _(미실행)_

### QA-3.5 I-04 교차 상품 — 다른 상품의 채팅 상대를 예약
- 행위: 상품 A 에만 채팅방이 있는 구매자를 상품 B 에 예약 시도
- 기대: 거부 (`isChatPartner` 는 (상품, 구매자)별)
- 가설: **통과 예상** (product 별 조회) — 교차 확인
- 실행 결과: _(미실행)_

---

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

## 예상 수확 (가설 요약 — 실행 전)

| 시나리오 | 가설 | 포트폴리오 가치 |
|---|---|---|
| QA-1.1 · 1.2 | 실패(버그) — 삭제 상품 예약·완료 | 높음 (`T-39`, "상태 vs 플래그" 설계 교훈) |
| QA-1.3 · 1.4 | 실패(버그) — 삭제·SOLD 상품 새 채팅방 | 높음 (요구사항-구현 갭, CH-01) |
| QA-1.6 (대조) | 통과 — 비대칭 드러냄 | 높음 (「개선」 논지의 축) |
| Tier 2 | 통과 — 동시성·멱등 탄탄 | 중 ("무엇이 견고한가" 근거) |
| Tier 3 | 대체로 통과, 일부 공백 | 중 (방어 커버리지 보강) |
