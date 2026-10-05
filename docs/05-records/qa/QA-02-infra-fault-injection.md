# QA-02 인프라 주입 실패 QA — 인프라 장애를 넣고 앱의 반응을 본다

- 관련: RabbitMQ(`ADR-0009`) · Redis(조회수) · MySQL(낙관적 락 `ADR-0008`) · S3(이미지) · WebSocket(`ADR-0007`)
- 작성일: 2026-10-02 · **실행일: 2026-10-05** (`T-41` · 이슈 #145)
- 목적: 애플리케이션 로직이 아니라 **인프라의 실패 모드**를 주입해(브로커 다운·연결 끊김·타임아웃)
  앱이 **장애를 격리하는가, 전파하는가**를 본다. "X 장애가 Y 요청/데이터를 망가뜨리나" 를 찾는다.
  [[QA-01-adversarial-scenarios]] 가 로직을 찔렀다면, 이 문서는 인프라를 찌른다.

## 실행 요약

| 구분 | 수 | 시나리오 |
|---|---|---|
| 결함 | **5** | A.1 → `T-44` · B.1 → `T-45` · B.4(신설) → `T-46` · D.1b · D.1c(신설) → `T-47` |
| 통과 | 7 | A.2 · A.3 · B.2 · B.3(주입 한계 있음) · C.1 · C.2 · D.1 |
| 실행하지 않음 / 재현 못함 | 3 | A.4(재현 못함) · D.2 · D.3(실행하지 않음) → `T-48` |
| **합계** | **15** | 시나리오 단위. 원래 문서 12개 + 실행 중 신설 3개(B.4 · D.1b · D.1c) |

**한 줄 결론** — 앱은 **연결이 끊겼을 때**의 장애는 잘 격리하고(알림 발행 실패 · 상세 응답), **끊기지도 정상이지도 않은 상태**와
**실패 뒤 복구**에는 약하다. 소비 실패는 무한 재시도되고(3초에 11,705회), Redis 무응답은 상세 응답을 120초 붙잡고,
플러시 중 DB 실패는 이미 꺼낸 조회수를 지운다. 세 결함 모두 "실패했을 때 무엇을 하나"가 코드에 없는 자리다.

**가설 대비** — 틀리거나 못 가린 것을 남긴다.

| 시나리오 | 가설 | 실제 |
|---|---|---|
| A.1 독약 재배달 | 실패(무한 재시도) | **맞음.** 단, "독약이 큐 선두를 막는다"는 덧붙인 가설은 **틀림** — 정상 알림은 옆에서 처리됐다 |
| B.1 Redis 장애 격리 | 통과(격리됨) | **틀림.** `DataAccessException` 을 삼키는 건 맞지만 무응답에서는 타임아웃이 60초라 지연이 전파된다 |
| B.3 복구 뒤 집계 | 통과(장애 중 유실) | **판정 못함.** `pause` 는 유실이 아니라 지연을 만든다 — 주입 방식의 한계 |
| C.2 ddl-auto 잔재 | 재현 불가 | **틀림.** `ALTER TABLE` 로 재현된다 |
| A.4 · D.2 · D.3 | 통과 예상 / 확인 필요 | **실행하지 못했다** (아래 각 절) |

실행 환경: Docker · Testcontainers(MySQL 8.4 · Redis · RabbitMQ 3-management) · `AWS_ACCESS_KEY=test AWS_SECRET_KEY=test` ·
브랜치 `test/145-qa-02-infra-fault`. 컨테이너를 건드리는 클래스는 `@DirtiesContext(AFTER_CLASS)` 로 격리했다.

## 쓰는 법

1. `test` 티켓으로 연다 (QA-01 과 같은 T-40 에 묶거나 별도). 실행 환경: Docker · Testcontainers · `AWS_ACCESS_KEY=test AWS_SECRET_KEY=test`
2. **장애 주입 방법** — 테스트 안에서 컨테이너를 멈추거나(`container.stop()`), 빈을 던지게 목킹하거나(`@MockitoBean` + `willThrow`), 잘못된 입력(미정의 enum)을 큐에 직접 넣는다
3. 기대(정상=격리) 동작을 단언 → 전파되면 테스트 실패 → 발견
4. 결과를 「실행 결과」에 채운다. 코드 리딩으로 세운 가설이 틀리면 그대로 적는다
5. 실패/위험은 「발견 기록」(QA-01 과 같은 문제→원인→해결→개선)으로

> ⚠️ 일부 시나리오는 Testcontainers 를 **의도적으로 멈춘다.** 같은 클래스의 다른 테스트에 영향 없게
> 격리하거나(별도 컨테이너 인스턴스) 마지막에 돌린다. `@DirtiesContext` 가 필요할 수 있다.

---

## Tier A — RabbitMQ (의심 가장 높음)

### QA-A.1 소비 실패 시 독약 메시지가 무한 재시도된다 (DLQ·재시도 제한 없음)
- 설정: `NotificationListener.onNotification` 이 던지게 만든다 — 예) `NotificationService.save` 를 `@MockitoBean` 으로 `willThrow`, 또는 미정의 타입이 담긴 메시지를 큐에 직접 발행(`NotificationType.valueOf` 가 `IllegalArgumentException`)
- 행위: 알림 하나 발행 → 소비 시도
- 기대(정상): 몇 번 재시도 후 **DLQ(dead-letter)로 격리**되거나 버려진다. 큐가 막히지 않는다
- 의심 근거: `RabbitConfig` 에 재시도 제한·DLQ·`RabbitListenerContainerFactory` 튜닝이 **전혀 없다.** Spring AMQP 기본은 예외 시 **requeue=true → 같은 메시지 무한 재배달**. 독약 메시지 하나가 소비자를 영원히 점유
- 가설: **실패/위험(=결함).** DLQ·재시도 상한이 없어 무한 루프. 포트폴리오 1순위 인프라 트러블슈팅
- 해결 방향(실행 후 적용): `Dead Letter Exchange` + `x-dead-letter-routing-key` 큐 선언, 리스너에 재시도 상한(`SimpleRetryPolicy`) 또는 `AmqpRejectAndDontRequeueException` 로 즉시 DLQ
- **실행 결과: 실패 — 가설대로 결함.** `NotificationPoisonMessageTest` · 소비 중 항상 예외가 나는 알림 한 건을 발행하고
  3초 뒤 `NotificationService.save` 호출 수를 세니 **11,705회**였다(초당 약 3,900). 같은 메시지를 쉬지 않고 다시 받아 처리한다.
  한계: 서비스를 mock 으로 던지게 했으므로 실패가 즉시 일어나는 상한 값이다. 실제 DB 장애라면 횟수는 줄지만 상한이 없다는 사실은 같다.
  **틀린 덧붙인 가설:** "독약이 선두에서 큐를 막아 뒤의 정상 알림이 처리되지 않는다" — 정상 알림은 독약 옆에서 그대로 소비됐다
  (`healthyMessageIsProcessedBehindPoison`, 통과). 피해는 큐 막힘이 아니라 CPU · 로그 · DB 호출을 쉬지 않고 쓰는 것이다.
  → 「발견 기록」 `[QA-A.1]`. `@Disabled` 로 `T-44` 에 연결

### QA-A.2 발행 실패해도 원 작업은 성공한다 (I-11)
- 설정: RabbitMQ 컨테이너 `stop()` 또는 `RabbitTemplate` 가 던지게
- 행위: `chatMessageService.send` / `completionService.complete`
- 기대(정상): 메시지 저장·거래완료 성공. 알림만 유실(로그)
- 의심 근거: `NotificationEventPublisher` 가 예외를 삼킨다
- 가설: **통과** (`TradeNotificationResilienceTest`·발행기 삼킴이 이미 덮음 — 실제 브로커 다운으로 재확인)
- **실행 결과: 통과 — 가설대로.** `NotificationBrokerOutageTest` · RabbitMQ 컨테이너를 실제로 `stop()` 했다
  (기존 `TradeNotificationResilienceTest` 는 발행기를 mock 으로 던지게 한 것이었다). 메시지는 저장되고 거래는 `SOLD` 로 성립한다.
  응답이 장애에 끌려 늦어지는지도 쟀다 — 전송 94ms · 거래완료 14ms(연결 거부가 즉시 실패한다). 통과가 공허하지 않은지
  임계값을 1ms 로 낮춰 실측했고 `알림 발행 실패 — 원 작업은 영향받지 않는다 (I-11)` 경고가 두 건 모두 찍혔다

### QA-A.3 커밋 ~ 발행 사이 장애 시 알림이 유실된다 (ADR-0009 가 수용한 대가)
- 설정: `@TransactionalEventListener(AFTER_COMMIT)` 가 도는 직전에 브로커를 내린다(타이밍 주입은 어려우니, 발행기 호출 직전 던지게)
- 행위: `reserve`/`complete` 커밋 성공 → 발행 실패
- 기대: 거래는 성립, 알림만 안 감. **이건 의도된 설계**(아웃박스 대신 커밋 후 발행). 유실이 실제로 일어나는지 관찰만
- 가설: **통과(의도된 유실).** 결함이 아니라 ADR-0009 의 트레이드오프 실측 — "왜 아웃박스를 안 썼나" 를 수치로 뒷받침
- **실행 결과: 통과 — 가설대로(의도된 유실).** A.2 와 같은 테스트에서 관찰했다. 거래완료 뒤 알림 테이블이 **비어 있다** — 유실이 실제로 일어난다.
  결함이 아니라 `ADR-0009` 가 커밋 후 발행을 고르며 받아들인 대가다. 유실된 알림을 되살릴 길이 없다는 뜻이므로 "아웃박스를 안 쓴 선택의 비용"을
  말할 근거가 된다. 다만 **"커밋과 발행 사이에 죽는" 타이밍은 주입하지 않았다** — 발행기를 못 닿게 한 상태로 대신 관찰했다

### QA-A.4 커넥션 끊김 후 리스너가 복구된다
- 설정: 소비 중 RabbitMQ 를 내렸다가 다시 올린다
- 행위: 다운 중 발행 몇 건 → 복구 후 소비되는지
- 기대: Spring AMQP 자동 재연결로 복구 후 밀린 메시지 소비
- 가설: **통과 예상**(기본 재연결) — 실측
- **실행 결과: 재현하지 못했다 — 통과도 실패도 아니다.** `docker restart` 로 브로커를 재시작하면 호스트 포트가 **다시 할당된다**
  (54499 → 54560). 앱의 연결 팩토리는 옛 포트를 계속 두드리므로 "앱이 복구되는가" 를 물을 수 없고, 단언은 복구 이전에 포트 불일치로 깨진다.
  Spring AMQP 의 자동 재연결은 **검증하지 못했다.** 고정 포트로 컨테이너를 띄우는 별도 구성이 필요하다 → `T-48`

---

## Tier B — Redis (조회수)

### QA-B.1 Redis 다운이 상품 상세를 막지 않는다 (장애 격리)
- 설정: Redis 컨테이너 `stop()`
- 행위: `productService.getProduct(productId, visitorId)` — 조회수 기록·합산이 Redis 를 탄다
- 기대(정상): 상세는 정상 응답, 조회수만 집계 안 됨(0 또는 DB 값)
- 의심 근거: `getProduct` 가 `record()`·`readTotal()` 로 Redis 를 **동기** 호출
- 가설: **통과(격리됨).** `ProductViewRedisRepository.recordView`·`findPendingCount` 가 `DataAccessException` 을 삼켜 false/0 반환 → 상세는 안 깨짐. **이미 방어돼 있음 = 견고성 근거**
- **실행 결과: 실패 — 가설이 틀렸다(부분).** `ProductViewRedisOutageTest` · `docker pause` 로 Redis 를 얼려 **무응답**을 주입했다(stop 하면 되살릴 수 없다).
  `DataAccessException` 을 삼키는 것은 맞다 — 상세는 응답하고 조회수는 DB 값(3)이다. 그러나 **설정에 타임아웃이 없어 Lettuce 기본 60초**다
  (`RedisTimeoutConfigTest` 가 `1M` 을 확인). 타임아웃을 지정하지 않은 실제 구성에서 상세 한 건이 Redis 를 두 번 불러(기록 · 합산) **120.1초** 걸렸다(실측).
  500ms 로 줄이면 5초 안에 응답한다. **연결 거부(포트 닫힘) 경로는 실행하지 않았다** — 거기서는 즉시 실패해 격리될 가능성이 높지만 확인하지 못했다.
  → 「발견 기록」 `[QA-B.1]`. `@Disabled` 로 `T-45` 에 연결

### QA-B.2 Redis 다운 중 플러시 스케줄러가 죽지 않는다
- 설정: Redis 다운 상태에서 조회수 플러시(`ProductViewCountScheduler`) 주기 도래
- 행위: 스케줄러 실행
- 기대: 예외를 삼키고 다음 주기에 재시도, 앱은 계속
- 가설: **확인 필요** — 삼킴이 record 경로엔 있으나 플러시 경로(`takePendingCount`·`findPendingProductIds`)에도 있는지 코드로 검증
- **실행 결과: 통과 — 가설의 "확인 필요" 가 가려졌다.** 코드 확인대로 `findPendingProductIds` · `takePendingCount` 는 `DataAccessException` 을 **삼키지 않는다.**
  Redis 가 멈춘 채 `flush()` 하면 예외가 그대로 나온다(현재 동작을 단언으로 고정). 다만 스프링 `@Scheduled` 는 반복 작업의 예외를 로그로 남기고 계속 돌아서,
  `ThreadPoolTaskScheduler` 로 같은 방식을 재현하니 Redis 가 돌아온 뒤 같은 스케줄러가 조회수 3 건을 DB 에 반영했다.
  한계: 실제 `ProductViewCountScheduler` 는 `@Profile("!test")` 라 테스트에서 못 띄운다 — 같은 로직을 직접 스케줄해서 확인했다

### QA-B.3 Redis 복구 후 집계가 이어진다
- 행위: 다운 중 조회 N건(유실) → 복구 후 조회 M건 → 집계 M 반영
- 기대: 복구 후 정상 집계(다운 중 유실은 수용)
- 가설: **통과 예상** — 실측
- **실행 결과: 판정하지 못했다 — 주입 방식의 한계.** 기대 3 에 **실제 5** 가 나왔다. `docker pause` 는 서버를 얼리기만 해서, 클라이언트가 타임아웃으로 포기한
  명령이 해동 뒤 서버에서 **실행된다.** "장애 중 기록이 유실된다" 를 이 방식으로는 재현하지 못한다. 복구 전후 3 건은 반영된다는 것만 확인하고 단언을 3~5 로 뒀다.
  유실을 재려면 연결 거부(컨테이너 stop)를 써야 한다

### QA-B.4 (신설) 플러시 중 DB 반영이 실패하면 조회수가 사라진다
- 설정: 미반영 조회수 3 건. `ProductRepository.increaseViewCount` 가 한 번 실패하는 플러셔
- 행위: 실패한 플러시 뒤에 정상 플러시
- 기대(정상): 실패한 주기의 조회수도 다음 주기에 반영된다 (3)
- 의심 근거: `ProductViewCountFlusher.flush` 는 `takePendingCount`(`getAndDelete`)로 Redis 에서 **먼저 꺼내고** DB 에 반영한다. 사이에서 실패하면 꺼낸 값이 어디에도 없다
- **실행 결과: 실패 — 결함.** `ProductViewRedisOutageTest.flush_doesNotLoseViewsWhenDatabaseFails` · 기대 `3L`, 실제 **`0L`**.
  QA 문서에 없던 갈래를 코드 리딩으로 의심해 추가했다. → 「발견 기록」 `[QA-B.4]`. `@Disabled` 로 `T-46` 에 연결

---

## Tier C — MySQL

### QA-C.1 낙관적 락 충돌은 재시도 없이 409 로 끝난다 (동작 명세)
- 설정: 동시 상태 전이(QA-2.x 와 동일)
- 행위: 진 요청의 결과
- 기대: `OptimisticLockingFailureException → 409 CONCURRENT_UPDATE`, **서버 재시도 없음**(클라이언트가 재시도)
- 가설: **통과(설계대로).** 재시도를 서버에 둘지 말지의 트레이드오프를 문서화 — ADR-0008 보강 근거
- **실행 결과: 통과 — 설계대로.** `OptimisticLockLoserTest` · 20명이 동시에 예약하면 패자 19명이 **낙관적 락 충돌 8 · 먼저 커밋된 뒤 상태 거부(`PRODUCT_NOT_ON_SALE`) 11** 로 갈린다.
  서버는 재시도하지 않는다(시도 수 == 요청 수 == 20). 충돌은 `GlobalExceptionHandler` 가 409 `CONCURRENT_UPDATE` 로 바꾼다. `ADR-0008` 이 재시도를 서버에 두지 않기로 한 선택이 실제로 어떻게 나타나는지 보여 준다.
  8 · 11 은 한 번의 실행값이고 실행마다 달라질 수 있다 — 단언은 "두 종류뿐" 과 "충돌이 1 건 이상" 만 건다

### QA-C.2 `ddl-auto: update` 컬럼 잔재 (테스트에선 안 드러나는 함정)
- 설정: 필드를 지운 뒤 기존 스키마에 `NOT NULL` 컬럼이 남은 상황 (예: `is_read` 이전의 `read`, 과거 제거 필드)
- 행위: 그 테이블에 INSERT
- 기대: 테스트는 매번 새 컨테이너라 **통과**하지만, 운영 DB(누적 스키마)에선 INSERT 가 깨질 수 있음 (`CLAUDE.md` 겪은 함정)
- 가설: **Testcontainers 로는 재현 불가** — 이건 "테스트가 못 잡는 자리"를 문서로 남기는 항목. 운영/스테이징에서 수동 확인하거나 마이그레이션 도구 도입을 개선안으로
- **실행 결과: 통과(함정 확인) — 가설이 틀렸다.** "Testcontainers 로 재현 불가" 라고 적었지만 **재현된다.** `ALTER TABLE products ADD COLUMN legacy_stock INT NOT NULL` 로 잔재를 흉내 내면
  상품 등록 INSERT 가 `Field 'legacy_stock' doesn't have a default value` 로 실패한다(`StaleSchemaColumnTest`). 매번 새 컨테이너를 쓰는 기존 테스트가 이 함정을 못 보는 것은 맞다 —
  일부러 만든 스키마로 확인했다. 결함 검출이 아니라 알려진 함정의 문서화이며, 해법(마이그레이션 도구)은 새 도구라 ADR 이 필요하다

---

## Tier D — S3 · WebSocket (주입 어려움, 시나리오만)

### QA-D.1 presigned URL 만료 / 업로드 미확정 고아 이미지
- 행위: presigned 발급 후 만료 시간 지나 업로드 / 발급만 받고 confirm 안 함
- 기대: 만료는 S3 가 거부(앱 무관), 미확정 이미지는 상품에 안 붙음(PD-02)
- 가설: **통과 예상** — 고아 이미지 정리 전략(없음)을 개선안으로 기록
- **실행 결과: 통과 — 그러나 코드를 읽다 더 큰 것을 찾았다.** `ProductImageConfirmTest.unconfirmedImageIsNotAttached` · 주소만 발급받고 확정하지 않으면 상품에 이미지가 없다.
  `confirmImages` 가 클라이언트가 보낸 주소를 **검증 없이** 저장하는 것을 의심해 두 시나리오를 더했고 둘 다 실패했다.
  - **D.1b (신설)** 외부 주소 `https://evil.example.com/tracker.png` 를 확정하면 거부 없이 저장된다. 컨트롤러 · DTO 에도 검증이 없다
  - **D.1c (신설)** 내 상품에 남의 상품 경로(`products/2/victim.png`)의 주소를 확정한 뒤 내 상품에서 지우면 `S3Template.deleteObject("ibid-product-images", "products/2/victim.png")` 가 호출된다
  → 「발견 기록」 `[QA-D.1b·1c]`. `@Disabled` 로 `T-47` 에 연결. 고아 이미지 정리 전략이 없다는 점은 그대로이고, 이번 범위에서는 다루지 않았다

### QA-D.2 WebSocket 연결 중 토큰 만료
- 행위: CONNECT 로 인증된 세션을 토큰 만료 시간 넘겨 유지하며 SUBSCRIBE/SEND
- 기대(불확실): 핸드셰이크 때만 검증하므로(ADR-0007) 연결 중 만료는 안 끊길 수 있음 — 의도인지 확인
- 가설: **통과(연결 유지)** 지만 보안상 재검토 거리 — ADR-0007 의 "연결 중 만료 처리는 구현에서" 를 실측
- **실행 결과: 실행하지 않았다.** 연결 뒤 토큰이 만료되는 상황은 만료 시간을 짧게 둔 토큰으로 CONNECT 한 뒤 시간을 넘겨 SUBSCRIBE / SEND 해야 하고, 표준 클라이언트(`WebSocketStompClient`)로 가능하다.
  이번에는 한 번에 시도하지 않았다 → `T-48`. 코드 리딩으로 얻은 사실만 적는다 — 토큰은 `StompAuthChannelInterceptor.preSend` 의 CONNECT 에서만 파싱하고, SUBSCRIBE 는 세션의 `Principal` 만 쓴다. **실행으로 확인한 것이 아니다**

### QA-D.3 비정상 종료 시 presence 누수
- 행위: SUBSCRIBE 후 DISCONNECT 없이 커넥션 강제 종료
- 기대: 세션 종료 감지로 presence 정리(안 그러면 영원히 "보는 중" → 알림 영구 억제)
- 의심 근거: `ChatPresenceRegistry` 는 DISCONNECT 프레임에만 정리. 네트워크 급단절(프레임 없음) 시 Spring 의 `SessionDisconnectEvent` 가 뜨는지, 인터셉터의 DISCONNECT 처리로 충분한지
- 가설: **확인 필요** — 급단절에서 누수 가능. 발견되면 세션 이벤트 리스너로 보강
- **실행 결과: 실행하지 않았다.** DISCONNECT 프레임 없이 TCP 를 끊으려면 표준 WebSocket 클라이언트로는 부족하고 하위 수준 소켓이 필요한데,
  이번 실행에서 그 방식으로 한 시도는 도중에 멈췄고 결과를 얻지 못했다(서버가 구독 단계에서 `ERROR` 프레임을 돌려줬다 — 원인은 확인하지 못했다).
  미완성 코드는 커밋하지 않고 지웠다 → `T-48`. 가설 쪽 단서는 코드 리딩이며 **검증되지 않았다**: Spring 은 세션이 끝나면 합성 DISCONNECT 메시지를 같은 인바운드 채널로 보내는 것으로 알고 있어,
  그렇다면 인터셉터의 `DISCONNECT` 분기가 불려 정리될 수 있다. 맞는지는 `T-48` 에서 실측한다

---

## 발견 기록

### [QA-A.1] 소비에 실패하는 알림이 무한 재배달된다

**문제** — 알림 한 건의 처리가 계속 예외를 내면 그 메시지가 쉬지 않고 다시 배달된다. 재현: `NotificationService.save` 가 항상 던지게 하고 알림 한 건을 발행해 3초를 둔다.
단언 `isLessThanOrEqualTo(5)` 가 `Expecting actual: 11705` 로 깨졌다 — **3초에 11,705회**(초당 약 3,900). 독약 하나가 소비자 스레드를 영원히 점유한다.

**원인** — 두 곳이 겹친다. `NotificationListener.onNotification`(`NotificationListener.java:19-23`)은 예외를 그대로 던지고, `RabbitConfig`(`RabbitConfig.java`)는 큐도 리스너 컨테이너도
재시도 정책 · dead-letter 설정 없이 기본으로만 선언한다. 이런 설정이 없으면 Spring AMQP 는 처리 중 예외를 requeue 로 이어 같은 메시지를 다시 배달한다.
`I-11`("알림 실패가 원 작업을 막지 않는다")은 **발행 쪽**만 지킨다 — 소비 쪽 실패는 어디에도 규칙이 없었다.

**해결** — 아직 고치지 않았다. 재현 테스트는 `NotificationPoisonMessageTest` 에 기대(정상) 단언으로 남고 `@Disabled("QA-A.1 … T-44")` 로 걸려 있다.
고칠 자리는 새 인프라 설정(dead-letter 큐 · 재시도 상한)이라 `ADR-0009` 보강 여부와 함께 **사람이 정한다** → `T-44`.

**개선** — `ADR-0009` 는 "알림 실패가 전송을 막지 않게" 라는 **발행** 요건에서 출발해 브로커를 골랐고, 소비 실패는 질문에 들어오지 않았다. 브로커를 도입하면 **실패한 메시지를 어디에 두나**가 따라오는 요건이다.
재발 방지는 새 큐를 만들 때마다 "소비가 실패하면?" 을 선언 시점에 묻는 것이다. 덧붙인 가설("선두를 막는다")이 틀렸던 점도 기록한다 — 위험은 막힘이 아니라 자원 소모이고, 모니터링 없이는 눈에 띄지 않는다.

### [QA-B.1] Redis 가 응답하지 않으면 상품 상세가 2분 걸린다

**문제** — Redis 가 죽은 것이 아니라 **멈췄을 때** 상품 상세 응답이 Redis 타임아웃만큼 붙잡힌다. 재현: Redis 컨테이너를 `pause` 하고 `productService.getProduct` 를 부른다. 타임아웃을 지정하지 않은 실제 구성에서 **120.1초**(실측).
`RedisTimeoutConfigTest` 는 명령 타임아웃을 단언했고 `Expecting actual: 1M to be less than or equal to: 3S` 로 깨졌다.

**원인** — `application.yml` 에 `spring.data.redis.timeout` 이 없어 Lettuce 기본 60초다. 상세 한 건이 Redis 를 두 번 부른다(`ProductViewCounter.record` → `readTotal`, `ProductService.java:108-109`).
`ProductViewRedisRepository` 가 `DataAccessException` 을 삼켜(`recordView` · `findPendingCount`) 격리했다고 믿었지만, **삼키는 것은 실패가 왔을 때의 이야기이고 무응답은 실패가 오기까지 시간이 걸린다.**

**해결** — 아직 고치지 않았다. `RedisTimeoutConfigTest` 가 `@Disabled("QA-B.1 … T-45")` 로 걸려 있다. 타임아웃은 설정 값 하나이고 500ms 로 줄이면 5초 안에 응답함을 확인했다 → `T-45`.

**개선** — "예외를 삼켜서 격리했다" 는 **빠른 실패**를 전제한다. 격리를 주장하는 코드는 **얼마나 오래** 붙잡히는지도 주장해야 한다. 조회수처럼 부가 기능이 핵심 응답 경로(상세)에 **동기로** 들어와 있으면 부가 기능의 지연이 핵심의 지연이 된다 —
타임아웃은 하한이고, 근본적으로는 조회수 기록을 응답 경로 밖으로 빼는 선택지가 있다(이 티켓 범위 밖).

### [QA-B.4] 조회수 플러시 중 DB 반영이 실패하면 조회수가 사라진다

**문제** — 미반영 조회수 3 건을 플러시하는데 DB 반영이 실패하면 그 3 건이 어디에도 남지 않는다. 기대 `3L`, 실제 **`0L`**.

**원인** — `ProductViewCountFlusher.flush`(`ProductViewCountFlusher.java:16-19`)는 `takePendingCount` 로 Redis 에서 값을 **먼저 꺼내고**(`ProductViewRedisRepository.takePendingCount` 의 `getAndDelete`)
그 다음 `increaseViewCount` 로 DB 에 반영한다. 사이에서 실패하면 꺼낸 값은 메모리 지역 변수에만 있다가 사라진다. "꺼내기" 와 "반영" 이 하나의 원자 단위가 아니다.

**해결** — 아직 고치지 않았다. `@Disabled("QA-B.4 … T-46")`. 선택지는 반영 성공 뒤에 지우거나(읽고 → 반영 → 차감), 반영 실패 시 되돌려 놓는 것 — `T-46` 에서 정한다.

**개선** — QA 문서에 없던 갈래다. QA-B.2 를 확인하다 플러시 경로를 읽으면서 의심했다. "한 번 꺼내면 사라지는" 연산을 외부 저장소 쓰기와 이어 붙이면 **실패 지점이 곧 유실 지점**이 된다. 조회수는 유실이 치명적이지 않지만 같은 모양이 중요한 값에 있을 수 있다.

### [QA-D.1b·1c] 이미지 주소를 검증 없이 저장하고, 그 주소로 남의 S3 객체를 지울 수 있다

**문제** — 이미지 확정(`confirmImages`)이 클라이언트가 보낸 주소를 그대로 저장한다. (b) 외부 주소가 거부 없이 저장된다. (c) 내 상품에 **남의 상품 경로의 주소**를 확정한 뒤 내 상품에서 지우면
`S3Template.deleteObject("ibid-product-images", "products/2/victim.png")` 가 호출된다 — **다른 상품의 S3 객체가 삭제된다.** 둘 다 기대 단언이 깨졌다(`Expecting code to raise a throwable.` / `NeverWantedButInvoked`).

**원인** — `ProductService.confirmImages`(`ProductService.java:38-41`)는 `findOwnedProduct` 로 "내 상품인가" 만 확인하고 "이 이미지 주소가 내 상품에 발급된 것인가" 는 보지 않는다.
`S3ImageUploader.delete`(`S3ImageUploader.java:39-42`)는 주소에서 키를 뽑아 소속을 보지 않고 바로 지운다. 소유자 확인의 **대상이 틀렸다** — 상품 소유와 이미지 소유는 다른 질문이다.
컨트롤러 · DTO 에도 주소 검증(`ImageConfirmRequest`)이 없다.

**해결** — 아직 고치지 않았다. `ProductImageConfirmTest` 두 시나리오가 `@Disabled("… T-47")` 로 걸려 있다. 선택지는 키 접두사 `products/{productId}/` 확인(단순) 또는 발급 기록과 대조(엄격). 실제 S3 는 mock 이라 **S3 객체가 지워지는 것까지는 확인하지 못했고, 삭제 호출이 일어난다는 것까지 확인했다.** → `T-47`.

**개선** — "인가 검사를 했다" 와 "올바른 대상에 인가 검사를 했다" 는 다르다. 값이 클라이언트에서 오고 그 값으로 **파괴적 행위**(삭제)를 하는 경로는 소유 검증이 그 값 자체에 닿아야 한다.
QA-D.1(발급만 받은 이미지)을 쓰다 코드를 읽고 찾았다 — 계획에 없던 발견이다.

## 수확 (실행 후)

| 시나리오 | 가설 | 실제 | 포트폴리오 가치 |
|---|---|---|---|
| QA-A.1 DLQ · 재시도 없음 | 실패(버그) | **실패 — 맞음.** 3초 11,705회 | 높음 — 독약 메시지, `T-44` |
| QA-A.2 · A.3 발행 실패 · 유실 | 통과(I-11 · 의도) | 통과 | 중 — `ADR-0009` 트레이드오프 실측 |
| QA-A.4 브로커 복구 | 통과 예상 | **재현 못함** (포트 재할당) | 낮음 — 못 가린 것을 남김 |
| QA-B.1 Redis 장애 격리 | 통과(이미 방어) | **실패 — 틀림.** 무응답 120초 | 높음 — "삼켰다 ≠ 격리" |
| QA-B.2 플러시 | 확인 필요 | 통과(플러시는 예외를 던지나 스케줄러가 살아남음) | 중 |
| QA-B.3 복구 뒤 집계 | 통과 | **판정 못함** (주입 방식 한계) | 낮음 |
| QA-B.4 플러시 중 DB 실패 (신설) | — | **실패 — 결함.** 3 → 0 | 높음 — 꺼내기와 반영의 비원자 |
| QA-C.1 락 충돌 | 통과(설계대로) | 통과 (충돌 8 · 상태 거부 11) | 중 — `ADR-0008` 보강 근거 |
| QA-C.2 ddl-auto 잔재 | 재현 불가 | **재현됨 — 틀림** | 중 — 마이그레이션 도구 개선안 |
| QA-D.1 미확정 이미지 | 통과 예상 | 통과 | 낮음 |
| QA-D.1b · D.1c 이미지 주소 (신설) | — | **실패 — 결함.** 외부 주소 저장 · 남의 객체 삭제 호출 | 높음 — 보안, 소유 검증의 대상 |
| QA-D.2 · D.3 WebSocket | 확인 필요 | **실행하지 않음** | — `T-48` |
