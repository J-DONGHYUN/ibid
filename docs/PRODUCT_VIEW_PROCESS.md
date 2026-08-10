# 상품 조회수 프로세스 (구현 상태)

`PROD-7` / `CACHE-2`의 조회수 부분 구현 기록이다. 기능 정의는 [FEATURE_SPEC.md](./FEATURE_SPEC.md) §2.3, 엔드포인트 명세는 [API.md](./API.md) §2 참고.

**요구사항**: 상품 상세 조회 시 조회수를 늘린다. **비로그인 사용자도 집계 대상**이므로 조회자 식별을 쿠키로 한다.

## 1. 설계 결정

| 관심사 | 선택 | 근거 |
| --- | --- | --- |
| 카운터 원본 | **Redis `INCR`** + 주기적 DB write-back | 조회 트래픽마다 DB에 쓰지 않는다. 읽기 위주 엔드포인트에 쓰기 트랜잭션이 붙는 구조를 피함 |
| 조회자 식별 | **`visitor_id` 쿠키(UUID) 하나로 통일** | 로그인 여부로 분기하지 않아 코드 경로가 하나. 공개 엔드포인트라 userId가 없어도 동작 |
| 중복 방지 | Redis `SET NX PX` (30분) | 새로고침 스팸 차단 |
| 중복 방지 + 증가 | **Lua 스크립트 1회 왕복** | 원자성이 필수 (§3) |
| 반영 대상 추적 | Redis Set (dirty set) | `KEYS`/`SCAN` 없이 반영할 상품만 O(1)로 추적 (§4) |
| 표시값 | `DB 누적값 + Redis 미반영 delta` | 내 조회가 즉시 반영돼 보임 |
| 비로그인 허용 | `@PublicApi` 메서드 애노테이션 | 경로 기반 제외는 HTTP 메서드를 구분하지 못함 (§6) |

**대안으로 검토했다가 택하지 않은 것**: `products.view_count`를 원본으로 두고 `update ... set view_count = view_count + 1`을 조회마다 실행하는 방식. 유실이 없고 스케줄러가 필요 없어 단순하지만, 중복 제거를 통과한 조회마다 인기 상품 한 행에 X락이 반복 걸린다. Redis 카운터로 가면 그 경합이 사라지고 `CACHE-2`의 Sorted Set 인기 랭킹으로 확장할 자리가 생긴다. 대가는 스케줄러 도입과 강제 종료 시 유실(§7)이다.

## 2. Redis 키

```
product-view::{productId}::{visitorId}   String "1", TTL 30분   중복 방지 마킹
product-view-count::{productId}          String, INCR 대상       미반영 delta
product-view-dirty                       Set of productId        반영 대기 목록
```

기존 `RefreshTokenRedisRepository`의 `"refresh-token::%s".formatted(...)` 네이밍을 따랐다. dirty set만 상품별이 아닌 **전역 단일 키**이며, 이 성질이 §7의 cluster mode 제약으로 이어진다.

## 3. 조회 기록 — 왜 Lua 스크립트인가

`src/main/resources/scripts/record-product-view.lua`

```lua
if redis.call('SET', KEYS[1], '1', 'NX', 'PX', ARGV[1]) then
    redis.call('INCR', KEYS[2])
    redis.call('SADD', KEYS[3], ARGV[2])
    return 1
end
return 0
```

`SET ... NX`는 성공 시 `OK`, 실패 시 nil을 반환하고 Lua에서 nil은 `false`이므로 `if`가 그대로 동작한다.

**애플리케이션에서 `SETNX` → `INCR` → `SADD`를 나눠 호출하면 안 되는 이유**:

- `SETNX` 직후 죽으면 그 방문자는 dedup 키 때문에 30분간 "이미 집계됨"으로 취급되지만 카운터는 오르지 않는다. 조회가 **조용히 유실되고 재시도로도 복구되지 않는다.**
- `SADD`만 실패하면 delta가 dirty set에 없어 flush 대상에서 빠지고, 그 상품을 누가 다시 볼 때까지 방치된다.

Redis는 스크립트를 단일 스레드로 통째 실행하므로 셋 다 실행되거나 전혀 실행되지 않는다. 부수 효과로 왕복이 3회 → 1회로 줄고, `RedisScript`를 `static final`로 잡아 `DefaultRedisScript`의 SHA 캐시가 유지돼 `EVALSHA`로 전송된다(실측: `number_of_cached_scripts:1`).

## 4. 반영(write-back) — 왜 `SREM` → `GETDEL` 순서인가

`ProductViewCountFlusher.flush()`

```java
for (Long productId : productViewCounter.readPendingProductIds()) {   // SMEMBERS
    long delta = productViewCounter.takePendingCount(productId);      // SREM → GETDEL
    if (delta > 0) {
        productRepository.increaseViewCount(productId, delta);        // 상품별 트랜잭션
    }
}
```

`GETDEL`을 먼저 하면, 그 사이 들어온 조회가 `INCR`+`SADD`로 카운터를 되살린 뒤 `SREM`이 dirty 멤버를 지워버린다. 카운터에 값이 있는데 flush가 찾을 수 없는 상태가 되어 **그 상품을 누가 다시 볼 때까지 반영이 무기한 지연**된다.

`SREM`을 먼저 하면 그 사이의 조회는 dirty에 다시 등록되므로 이번 `GETDEL`에 잡히거나 다음 주기에 잡힌다. 남는 것은 카운터 없는 유령 멤버뿐이고, 다음 주기에 `GETDEL`이 nil을 반환해 `delta = 0`이 되고 `if (delta > 0)` 가드가 걸러낸다. **stale 멤버가 스스로 정리되므로 dirty set에 TTL이 필요 없다.**

**`GETDEL`이 상호배제 지점이다.** 값을 읽으면서 동시에 지우는 원자 연산이라 어떤 delta도 정확히 한 번만 넘겨진다. 덕분에 flush가 동시에 두 개 돌아도(주기 flush 중 `SIGTERM`이 오면 shutdown flush가 겹칠 수 있다) 한쪽만 값을 가져가고 다른 쪽은 `delta = 0`으로 빠져 **중복 집계가 구조적으로 불가능하다.**

**상품별 트랜잭션인 이유**: `@Modifying` 쿼리에 `@Transactional`을 붙여 호출마다 트랜잭션이 열린다. 전체를 한 트랜잭션으로 묶으면 한 상품의 UPDATE 실패로 롤백될 때 `GETDEL`은 이미 실행된 상태라 그 배치의 delta가 통째로 사라진다. 건별로 끊으면 실패한 상품 하나만 잃는다.

`@Modifying(flushAutomatically = true, clearAutomatically = true)` — 벌크 JPQL은 영속성 컨텍스트를 우회하므로 실행 전 flush로 미반영 변경을 밀어내고, 실행 후 clear로 stale 엔티티를 막는다.

## 5. 종료 시 마지막 flush — `@PreDestroy`는 동작하지 않는다

처음엔 `@PreDestroy`로 마지막 flush를 넣었는데 앱을 띄워보니 종료 시 매번 실패했다.

```
Destroy method on bean 'productViewCountScheduler' threw an exception:
InvalidDataAccessApiUsageException: LettuceConnectionFactory has been STOPPED. Use start() to initialize it
```

`LettuceConnectionFactory`는 `SmartLifecycle` 빈이라 **destroy 콜백보다 먼저 stop**된다. `AbstractApplicationContext.doClose()`의 순서가 `ContextClosedEvent 발행` → `lifecycleProcessor.onClose()` → `destroyBeans()` 이므로, **`@EventListener(ContextClosedEvent.class)`** 로 바꿔야 Redis와 DataSource가 모두 살아 있는 시점에 실행된다.

여기에 `ThreadPoolTaskScheduler`의 `waitForTasksToCompleteOnShutdown(true)` + `server.shutdown: graceful`을 함께 두어 진행 중인 flush와 요청이 마무리되게 했다.

## 6. 비로그인 접근

`AuthenticationInterceptor`가 `/**`에 걸려 있고 `WebConfig.PUBLIC_ENDPOINTS`에 `/api/auth/**`만 있어서, 비로그인 사용자는 상품 상세를 **호출조차 못 하는 상태**였다(`API.md`는 이미 "인증 X"로 적혀 있어 문서-코드 불일치).

**경로 기반 `excludePathPatterns("/api/products/**")`를 쓰지 않았다.** 경로 제외는 HTTP 메서드를 구분하지 못해 `POST /api/products`(등록)와 `PATCH /api/products/{id}/on-sale`까지 열린다. 대신 기존 `@AdminOnly` 처리 메커니즘을 미러링해 `@PublicApi` 메서드 애노테이션을 만들고 `WebConfig`는 건드리지 않았다.

**`@PublicApi`의 토큰 처리**: 헤더가 없으면 통과시키지만, **토큰을 보냈다면 유효해야 한다**(만료·위조는 401). 무효 토큰까지 삼키면 프론트의 `401 → refresh → 재시도` 흐름에서 만료 토큰이 숨어버린다.

함께 `@LoginUser(required = false)`를 추가했다. 기존 `LoginUserArgumentResolver`는 `userInfo`가 없으면 무조건 던져서 `@LoginUser`를 optional로 쓸 방법이 없었고, 공개 엔드포인트에 실수로 붙이면 401이 났다. 이번 조회수 기능은 userId를 쓰지 않지만(중복 방지가 쿠키 기반), `@PublicApi`가 만드는 구멍을 메우고 향후 "로그인했으면 개인화" 형태의 공개 API 길을 열어둔 것이다.

## 7. 알려진 이슈 / 남은 리스크

1. **강제 종료 시 delta 유실** — `GETDEL` 후 DB 커밋 전에 `SIGKILL`·크래시가 나면 그 구간을 잃는다. 정상 종료는 §5로 막았고 flush 주기 60초로 창을 좁혔다. 조회수는 과금이 아닌 참여 지표이므로 유실을 택했다(반대 순서면 중복 집계).
2. **Redis 장애** — `recordView`는 `false`(증가 생략, fail-closed), `findPendingCount`는 `0`(DB 누적값만 노출). 상세 응답은 정상 200. `catch (DataAccessException)` 범위를 Redis 호출로만 한정해 다른 예외를 삼키지 않는다.
3. **표시값의 일시적 과소집계** — `readTotal`이 DB와 Redis를 따로 읽으므로, 그 사이 flush가 끼면 delta만큼 낮은 값이 한 번 보인다(수 ms). **과다 집계는 구조적으로 불가능**하다 — `GETDEL`이 `UPDATE`보다 먼저 실행돼 커밋 시점에 그 delta는 이미 Redis에 없다.
4. **쿠키를 보내지 않는 클라이언트**(curl·봇) — 매 요청 새 UUID를 받아 중복 방지가 무력화된다. 후속 강화안: 키에 해시된 IP+UA 혼합, IP 단위 레이트리밋.
5. **React StrictMode 이중 effect / 최초 동시 요청** — 쿠키가 아직 없으면 서로 다른 UUID를 받아 2회 카운트될 수 있다. 최초 방문 한정 오차.
6. **Redis Cluster `CROSSSLOT`** — Lua 스크립트가 만지는 3개 키가 다른 슬롯에 떨어지면 실패한다. dirty set이 전역 단일 키라 해시 태그로 슬롯을 맞출 수 없다. `AWS-3`(ElastiCache cluster mode) 시점의 탈출구는 **`SADD`를 스크립트 밖으로 빼고**(실패 시 유실이 아니라 다음 조회까지 지연) dedup·카운터 키만 `{productId}` 해시 태그로 묶는 것.
7. **`viewCount` 컬럼 추가가 `ddl-auto: update`에 의존** — 데이터가 있는 환경은 명시적 마이그레이션이 필요하다(Flyway 미도입).
8. **정책** — 판매자 본인 조회도 집계한다(userId를 보지 않음). 목록 응답에는 조회수를 넣지 않았다(16건마다 Redis 왕복 — 필요해지면 `MGET`).

## 8. 설정

```yaml
server:
  shutdown: graceful

product:
  view:
    dedup-ttl: ${PRODUCT_VIEW_DEDUP_TTL:PT30M}
    cookie-max-age: ${PRODUCT_VIEW_COOKIE_MAX_AGE:31536000}
    flush-interval: ${PRODUCT_VIEW_FLUSH_INTERVAL:60000}
```

**`cookie-max-age`는 `dedup-ttl`보다 반드시 길어야 한다.** 쿠키가 먼저 만료되면 매 요청 새 UUID가 발급돼 중복 방지가 무력화된다.

쿠키 속성은 `RefreshTokenCookieHandler`와 동일하게 `HttpOnly`·`Secure`·`Path=/`·`SameSite=Strict`. `localhost:3000 → localhost:8080`은 포트가 달라 cross-origin이지만 SameSite는 포트를 무시한 registrable domain 기준이라 **same-site**이고, 브라우저가 `http://localhost`를 secure context로 취급하므로 dev에서도 저장·전송된다.

## 9. 검증

`./gradlew test` — 248개 통과. 조회수 관련 테스트:

| 테스트 | 검증 |
| --- | --- |
| `ProductViewRedisRepositoryTest` | Lua 스크립트 동작 — 첫 조회 true·카운터 1·dirty 등록·TTL 30분, **같은 방문자 재조회 시 카운터 불변**(세 명령이 함께 건너뛰어짐), `takePendingCount`가 카운터·dirty 멤버를 함께 정리 |
| `ProductViewConcurrencyTest` | 서로 다른 방문자 30명 동시 조회 → flush → `viewCount == 30`(유실 0) / **같은 방문자 30명 동시 조회 → `viewCount == 1`**(Lua 원자성) |
| `ProductViewCountFlusherTest` | delta가 DB에 반영됨 / dirty 비었으면 no-op / **두 번 호출해도 중복 반영 없음** / 여러 상품이 각자 상품에 반영됨 |
| `ProductViewIntegrationTest` | 비로그인 첫 조회 1 / 같은 방문자 재조회 불변 / 다른 방문자 증가 / **없는 상품은 Redis에 키가 하나도 안 생김** |
| `ProductControllerTest` | 응답 `viewCount` / 쿠키 없으면 발급(`HttpOnly`·`Path=/`·`SameSite=Strict`) / 기존 쿠키 재사용 / **404엔 Set-Cookie 없음** |
| `AuthenticationInterceptorTest` | `@PublicApi` + 토큰 없음 → 통과·속성 null / 유효 토큰 → 속성 설정 / **만료·위조 토큰 → 401** |
| `LoginUserArgumentResolverTest` | `required=true`(기본) 부재 시 401 / `required=false` 부재 시 `null` |

실제 앱 기동 확인 (`bootTestRun`):

```
비로그인 상품 상세      → 200 + Set-Cookie: visitor_id=...; Secure; HttpOnly; SameSite=Strict
같은 visitor 2회        → viewCount 2 → 2            (30분 중복 방지)
다른 visitor            → viewCount 3
없는 상품               → 404, Set-Cookie 없음, Redis 흔적 0
공개 API + 잘못된 토큰   → 401
POST /api/products      → 401                        (쓰기 API 회귀 확인)
PATCH .../on-sale       → 401
GET  /api/products      → 200

Redis: dedup 키 pttl 1779964ms(≈29.7분), number_of_cached_scripts:1
```

종료 시 flush 검증 — 외부 Redis를 앱보다 오래 살리고 `flush-interval`을 1시간으로 둬 주기 flush를 배제한 뒤 `SIGTERM`:

```
조회 2건 기록 → SIGTERM → 종료 후 Redis:
  product-view-count::1        = (빈 값)   ← shutdown flush가 소비
  product-view-dirty           = (빈 값)   ← 소비
  product-view::1::ext-a/ext-b = 유지      ← dedup 키는 건드리지 않음 (정상)
  종료 중 에러 0건
```

## 10. 관련 파일

**조회수 집계**

- `product/domain/Product.java` — `viewCount` 컬럼
- `product/infra/ProductRepository.java` — `increaseViewCount(id, delta)` 벌크 UPDATE
- `product/infra/ProductViewRedisRepository.java` — Lua 실행, dirty set, `takePendingCount`
- `src/main/resources/scripts/record-product-view.lua`
- `product/application/ProductViewCounter.java` — dedup TTL 보유, 표시값 합산
- `product/application/ProductViewCountFlusher.java` — write-back 루프
- `product/application/ProductViewCountScheduler.java` — 주기 트리거 + 종료 시 flush (`@Profile("!test")`)
- `common/config/SchedulingConfig.java` — `@EnableScheduling` + `ThreadPoolTaskScheduler`

**조회 연동 · 쿠키**

- `product/application/ProductService.java` — `getProduct(productId, visitorId)`
- `product/presentation/ProductController.java` — `@CookieValue` + `Set-Cookie`
- `product/presentation/cookie/ProductViewCookieHandler.java`
- `product/presentation/dto/ProductDetailResponse.java`

**비로그인 접근**

- `auth/presentation/interceptor/PublicApi.java`
- `auth/presentation/interceptor/AuthenticationInterceptor.java`
- `auth/presentation/resolver/LoginUser.java` · `LoginUserArgumentResolver.java`
