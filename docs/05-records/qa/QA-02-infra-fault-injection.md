# QA-02 인프라 주입 실패 QA — 인프라 장애를 넣고 앱의 반응을 본다

- 관련: RabbitMQ(`ADR-0009`) · Redis(조회수) · MySQL(낙관적 락 `ADR-0008`) · S3(이미지) · WebSocket(`ADR-0007`)
- 작성일: 2026-10-02
- 목적: 애플리케이션 로직이 아니라 **인프라의 실패 모드**를 주입해(브로커 다운·연결 끊김·타임아웃)
  앱이 **장애를 격리하는가, 전파하는가**를 본다. "X 장애가 Y 요청/데이터를 망가뜨리나" 를 찾는다.
  [[QA-01-adversarial-scenarios]] 가 로직을 찔렀다면, 이 문서는 인프라를 찌른다.

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
- 실행 결과: _(미실행)_

### QA-A.2 발행 실패해도 원 작업은 성공한다 (I-11)
- 설정: RabbitMQ 컨테이너 `stop()` 또는 `RabbitTemplate` 가 던지게
- 행위: `chatMessageService.send` / `completionService.complete`
- 기대(정상): 메시지 저장·거래완료 성공. 알림만 유실(로그)
- 의심 근거: `NotificationEventPublisher` 가 예외를 삼킨다
- 가설: **통과** (`TradeNotificationResilienceTest`·발행기 삼킴이 이미 덮음 — 실제 브로커 다운으로 재확인)
- 실행 결과: _(미실행)_

### QA-A.3 커밋 ~ 발행 사이 장애 시 알림이 유실된다 (ADR-0009 가 수용한 대가)
- 설정: `@TransactionalEventListener(AFTER_COMMIT)` 가 도는 직전에 브로커를 내린다(타이밍 주입은 어려우니, 발행기 호출 직전 던지게)
- 행위: `reserve`/`complete` 커밋 성공 → 발행 실패
- 기대: 거래는 성립, 알림만 안 감. **이건 의도된 설계**(아웃박스 대신 커밋 후 발행). 유실이 실제로 일어나는지 관찰만
- 가설: **통과(의도된 유실).** 결함이 아니라 ADR-0009 의 트레이드오프 실측 — "왜 아웃박스를 안 썼나" 를 수치로 뒷받침
- 실행 결과: _(미실행)_

### QA-A.4 커넥션 끊김 후 리스너가 복구된다
- 설정: 소비 중 RabbitMQ 를 내렸다가 다시 올린다
- 행위: 다운 중 발행 몇 건 → 복구 후 소비되는지
- 기대: Spring AMQP 자동 재연결로 복구 후 밀린 메시지 소비
- 가설: **통과 예상**(기본 재연결) — 실측
- 실행 결과: _(미실행)_

---

## Tier B — Redis (조회수)

### QA-B.1 Redis 다운이 상품 상세를 막지 않는다 (장애 격리)
- 설정: Redis 컨테이너 `stop()`
- 행위: `productService.getProduct(productId, visitorId)` — 조회수 기록·합산이 Redis 를 탄다
- 기대(정상): 상세는 정상 응답, 조회수만 집계 안 됨(0 또는 DB 값)
- 의심 근거: `getProduct` 가 `record()`·`readTotal()` 로 Redis 를 **동기** 호출
- 가설: **통과(격리됨).** `ProductViewRedisRepository.recordView`·`findPendingCount` 가 `DataAccessException` 을 삼켜 false/0 반환 → 상세는 안 깨짐. **이미 방어돼 있음 = 견고성 근거**
- 실행 결과: _(미실행)_

### QA-B.2 Redis 다운 중 플러시 스케줄러가 죽지 않는다
- 설정: Redis 다운 상태에서 조회수 플러시(`ProductViewCountScheduler`) 주기 도래
- 행위: 스케줄러 실행
- 기대: 예외를 삼키고 다음 주기에 재시도, 앱은 계속
- 가설: **확인 필요** — 삼킴이 record 경로엔 있으나 플러시 경로(`takePendingCount`·`findPendingProductIds`)에도 있는지 코드로 검증
- 실행 결과: _(미실행)_

### QA-B.3 Redis 복구 후 집계가 이어진다
- 행위: 다운 중 조회 N건(유실) → 복구 후 조회 M건 → 집계 M 반영
- 기대: 복구 후 정상 집계(다운 중 유실은 수용)
- 가설: **통과 예상** — 실측
- 실행 결과: _(미실행)_

---

## Tier C — MySQL

### QA-C.1 낙관적 락 충돌은 재시도 없이 409 로 끝난다 (동작 명세)
- 설정: 동시 상태 전이(QA-2.x 와 동일)
- 행위: 진 요청의 결과
- 기대: `OptimisticLockingFailureException → 409 CONCURRENT_UPDATE`, **서버 재시도 없음**(클라이언트가 재시도)
- 가설: **통과(설계대로).** 재시도를 서버에 둘지 말지의 트레이드오프를 문서화 — ADR-0008 보강 근거
- 실행 결과: _(미실행)_

### QA-C.2 `ddl-auto: update` 컬럼 잔재 (테스트에선 안 드러나는 함정)
- 설정: 필드를 지운 뒤 기존 스키마에 `NOT NULL` 컬럼이 남은 상황 (예: `is_read` 이전의 `read`, 과거 제거 필드)
- 행위: 그 테이블에 INSERT
- 기대: 테스트는 매번 새 컨테이너라 **통과**하지만, 운영 DB(누적 스키마)에선 INSERT 가 깨질 수 있음 (`CLAUDE.md` 겪은 함정)
- 가설: **Testcontainers 로는 재현 불가** — 이건 "테스트가 못 잡는 자리"를 문서로 남기는 항목. 운영/스테이징에서 수동 확인하거나 마이그레이션 도구 도입을 개선안으로
- 실행 결과: _(미실행 — 재현 전략 필요)_

---

## Tier D — S3 · WebSocket (주입 어려움, 시나리오만)

### QA-D.1 presigned URL 만료 / 업로드 미확정 고아 이미지
- 행위: presigned 발급 후 만료 시간 지나 업로드 / 발급만 받고 confirm 안 함
- 기대: 만료는 S3 가 거부(앱 무관), 미확정 이미지는 상품에 안 붙음(PD-02)
- 가설: **통과 예상** — 고아 이미지 정리 전략(없음)을 개선안으로 기록
- 실행 결과: _(미실행)_

### QA-D.2 WebSocket 연결 중 토큰 만료
- 행위: CONNECT 로 인증된 세션을 토큰 만료 시간 넘겨 유지하며 SUBSCRIBE/SEND
- 기대(불확실): 핸드셰이크 때만 검증하므로(ADR-0007) 연결 중 만료는 안 끊길 수 있음 — 의도인지 확인
- 가설: **통과(연결 유지)** 지만 보안상 재검토 거리 — ADR-0007 의 "연결 중 만료 처리는 구현에서" 를 실측
- 실행 결과: _(미실행)_

### QA-D.3 비정상 종료 시 presence 누수
- 행위: SUBSCRIBE 후 DISCONNECT 없이 커넥션 강제 종료
- 기대: 세션 종료 감지로 presence 정리(안 그러면 영원히 "보는 중" → 알림 영구 억제)
- 의심 근거: `ChatPresenceRegistry` 는 DISCONNECT 프레임에만 정리. 네트워크 급단절(프레임 없음) 시 Spring 의 `SessionDisconnectEvent` 가 뜨는지, 인터셉터의 DISCONNECT 처리로 충분한지
- 가설: **확인 필요** — 급단절에서 누수 가능. 발견되면 세션 이벤트 리스너로 보강
- 실행 결과: _(미실행)_

---

## 예상 수확 (가설 요약 — 실행 전)

| 시나리오 | 가설 | 포트폴리오 가치 |
|---|---|---|
| QA-A.1 RabbitMQ DLQ·재시도 없음 | **실패/위험(버그)** | 높음 — 독약 메시지, DLQ 도입이 「해결」 |
| QA-A.2·A.3 발행 실패/유실 | 통과(I-11·의도) | 중 — ADR-0009 트레이드오프 실측 |
| QA-B.1 Redis 장애 격리 | 통과(이미 방어) | 중 — "무엇이 견고한가" |
| QA-B.2·D.3 플러시·presence 누수 | 확인 필요 | 중 — 숨은 공백 가능 |
| QA-C.2 ddl-auto 잔재 | 테스트 사각지대 | 중 — 마이그레이션 도구 개선안 |
| QA-D.2 WS 토큰 만료 | 통과(설계) | 중 — 보안 재검토 |
