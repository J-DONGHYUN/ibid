# 대량 더미데이터 시딩 (PERF-1)

> **이 문서 = 측정 기반의 첫 조각.** 로드맵상 위치는 [`ROADMAP.md` Phase 0](./ROADMAP.md#phase-0-측정-기반-선행-필수).
> 축 1~3의 before/after 숫자는 모두 여기서 만든 데이터 위에서 재야 의미가 있다.

## 1. 왜 필요했나

데이터가 수천 행이면 인덱스를 걸어도 실행계획이 바뀌지 않는다. `EXPLAIN`이 풀스캔과 인덱스 스캔을 구분해주지 않고, p95도 전부 1ms 아래로 뭉개진다. **"빨라졌습니다"를 증명할 수 없는 상태**를 먼저 없애는 작업이다.

## 2. 실행 방법

```bash
SEED_REWRITE_BATCHED_STATEMENTS=true \
  ./gradlew bootTestRun --args='--seed.enabled=true --seed.products=2000000'
```

`bootTestRun`은 Testcontainers로 MySQL 8.4 + Redis를 띄우고 앱을 실행하는 개발 진입점이다. 시더는 **테스트 소스**(`src/test/java/.../support/seed/`)에 있어 프로덕션 jar에 들어가지 않고, `seed.enabled=true`가 없으면 빈 자체가 등록되지 않는다.

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `seed.enabled` | `false` | 켜지 않으면 시더 미등록 |
| `seed.products` | 2,000,000 | 상품 행수 |
| `seed.users` · `seed.tags` | 10,000 · 200 | |
| `seed.product-images` | 3,000,000 | |
| `seed.product-tags` | 4,000,000 | `product_tag` 조인 행수 |
| `seed.product-likes` | 1,000,000 | |
| `seed.orders` | 300,000 | |
| `seed.batch-size` | 1,000 | JDBC 배치 크기 |
| `seed.jpa-baseline-rows` | 20,000 | `saveAll` 기준선 측정용(측정 후 삭제) |
| `seed.random-seed` | 20260811 | **고정 시드** — 재실행 시 같은 데이터 |
| `seed.recreate-like-index-after-seed` | `false` | 찜 인덱스를 시딩 후에 생성 |
| `SEED_REWRITE_BATCHED_STATEMENTS` (env) | `false` | JDBC URL 파라미터. 기본 off라 기존 테스트 동작 불변 |

## 3. 만들어지는 데이터

`ddl-auto: update`가 만든 스키마 위에 **명시적 id**로 넣는다. id가 1..N으로 확정되니 테이블 간 참조를 조회 없이 맞출 수 있다.

| 테이블 | 행수 | 분포 |
| --- | --- | --- |
| `users` | 10,000 | 비밀번호는 전원 `pass1234`(부하테스트 로그인용), BCrypt 해시는 1회만 계산해 재사용 |
| `tags` | 200 | |
| `products` | 2,000,000 | 상태 `ON_SALE 80% / SOLD_OUT 15% / PENDING 5%` · 등급 `USED 55% / LIKE_NEW 34% / NEW 10%` |
| `product_images` | 3,000,000 | 상품에 무작위 배정 |
| `product_tag` | 4,000,000 | 상품당 1~4개, 상품 내 태그 중복 없음 |
| `product_likes` | 1,000,000 | **`(user_id, product_id)` 유니크 보장** |
| `orders` | 300,000 | `seller_id`가 실제 상품 판매자와 일치, `total_price = price × quantity` |

**분포를 일부러 편중시켰다.** 균등분포는 현실과 다르고 실행계획도 다르게 나온다.

- 판매자: 상위 소수가 대량 보유 (10,000명 중 최다 보유자가 수천 건)
- 조회수: 롱테일 (0 ~ 약 100,000, 평균 8,700)
- 등록시각: 최근 2년, 최근일에 가중
- 찜: 인기 상품 편중 (1위 상품 7,750개 — 유저 수 10,000에 근접)
- 가격: 10,000원 ~ 500만원, 저가 쪽에 가중

### 검증 결과

| 항목 | 결과 |
| --- | --- |
| 행수 | 7개 테이블 전부 지정값과 일치 |
| 찜 유니크 | 중복쌍 0 (`count(distinct user-product) == count(*)`) |
| 시드 고정 | 재실행 시 동일 데이터 |
| 로컬 계정 | `test@test.test` 등 3개가 시딩 이후 정상 추가 |

## 4. 측정 결과 — 1,030만행 전체 시딩

측정 조건: MySQL 8.4 (Docker, Testcontainers) · `batchSize=1000` · `rewriteBatchedStatements=true` · `FOREIGN_KEY_CHECKS=0` · 단일 커넥션

| 단계 | 행수 | 소요 | rows/sec |
| --- | --- | --- | --- |
| `products` (jpa `saveAll` 기준선) | 20,000 | 4.0s | **5,022** |
| `users` | 10,000 | 0.2s | 51,546 |
| `tags` | 200 | 0.0s | 50,000 |
| **`products` (jdbc 배치)** | 2,000,000 | 49.7s | **40,210** |
| `product_images` | 3,000,000 | 246.5s | 12,169 |
| `product_tag` | 4,000,000 | 40.8s | 97,964 |
| `product_likes` | 1,000,000 | 120.4s | 8,302 |
| `orders` | 300,000 | 3.3s | 89,820 |
| **합계** | **10,310,200** | **465.0s** | |

### 발견 ① JPA `saveAll`은 배치가 걸리지 않는다 — 8배 차이

같은 `products` 테이블에 **5,022 rows/sec vs 40,210 rows/sec**.

원인은 `Product`의 `@GeneratedValue(strategy = IDENTITY)`다. IDENTITY는 INSERT를 실행해야 PK를 알 수 있어 Hibernate가 행마다 즉시 INSERT를 보낸다. `batch_size`를 키워도 소용이 없다. 시딩처럼 PK를 미리 정해도 되는 작업에서는 JPA를 우회하는 게 맞다.

### 발견 ② 같은 배치 방식인데 단계별로 12배 차이가 난다

`product_tag` 97,964 rows/sec vs `product_likes` 8,302 rows/sec. 배치 크기도, 커넥션도, FK 설정도 같다.

갈린 지점은 **보조 인덱스에 값이 들어가는 순서**다.

| 단계 | 참조 키 순서 | 보조 인덱스 | rows/sec |
| --- | --- | --- | --- |
| `product_tag` | `product_id` **순차 증가** | 없음(FK 인덱스만) | 97,964 |
| `products` | PK 순차 | 없음 | 40,210 |
| `product_images` | `product_id` **무작위** | `(product_id, sort_order)` | 12,169 |
| `product_likes` | `product_id` **무작위** | 유니크 `(user_id, product_id)` + `(product_id)` | 8,302 |

순차 삽입은 인덱스 오른쪽 끝에만 붙어 페이지 분할이 거의 없다. 무작위 삽입은 B-tree 곳곳에 끼어들어 페이지 분할과 랜덤 I/O를 유발한다. 보조 인덱스가 2개인 `product_likes`가 가장 느린 것도 같은 이유다.

> 이 차이는 시딩만의 문제가 아니다. **운영에서도 무작위 키(UUID 등)를 PK로 쓰면 같은 비용을 낸다.**

### 발견 ③ 배치 크기는 `rewriteBatchedStatements`가 켜져 있을 때만 의미가 있다

`products` 50만행 고정, 다른 단계 제외. 매 조합마다 새 컨테이너로 시작.

| `rewriteBatchedStatements` | `batchSize` | 소요 | rows/sec | 기준 대비 |
| --- | --- | --- | --- | --- |
| `false` | 1,000 | 65.4s | 7,640 | 1.00x |
| `false` | 5,000 | 64.2s | 7,791 | 1.02x |
| `true` | 100 | 17.7s | 28,203 | 3.69x |
| **`true`** | **1,000** | **12.4s** | **40,163** | **5.26x** |
| `true` | 5,000 | 13.6s | 36,867 | 4.83x |

**`false`일 때는 배치 크기를 5배 늘려도 2%밖에 안 변한다.** 1,000과 5,000이 사실상 같다.

이유는 `addBatch()`가 하는 일이 드라이버 설정에 따라 다르기 때문이다.

```
rewriteBatchedStatements=false
  → 드라이버가 INSERT ... VALUES (...) 를 500,000번 그대로 보낸다.
    배치는 왕복 대기를 조금 줄여주는 정도에 그친다.

rewriteBatchedStatements=true
  → 드라이버가 배치를 하나로 합쳐 보낸다.
    INSERT ... VALUES (...),(...),(...) ... × batchSize
    전송 횟수와 서버 파싱 횟수가 batchSize배로 줄어든다.
```

즉 **`batchUpdate`를 쓰는 것만으로는 거의 못 번다.** off 상태의 7,640 rows/sec는 JPA `saveAll` 기준선(5,022)과 큰 차이가 없다. 실제로 버는 건 드라이버가 SQL을 합쳐줄 때부터다.

배치 크기는 1,000이 최적이고 5,000에서 오히려 떨어졌다. 합쳐진 SQL 문자열이 커지면 드라이버 쪽 버퍼 복사와 서버 파싱 비용이 늘어나기 때문으로, "크게 잡을수록 좋다"가 성립하지 않는 구간이 있다.

> **정리**: 흔히 "JPA 대신 JDBC 배치를 쓰면 빨라진다"고 말하지만, 이 프로젝트에서 8배 차이의 실제 원인은 **① `IDENTITY` 때문에 JPA가 배치를 못 걸었던 것**과 **② 드라이버가 SQL을 합쳐준 것** 두 개다. `batchUpdate` 호출 자체의 기여는 작다.

## 5. 이 데이터로 바로 확인한 "before"

시딩 직후 같은 DB에서 측정. 응답시간은 `docker exec` 오버헤드(약 68ms)를 포함한 값이며, 괄호 안이 차감치다.

| 쿼리 | `EXPLAIN` | 실측 |
| --- | --- | --- |
| 커서: `WHERE id < ? ORDER BY id DESC LIMIT 16` | `range` + Backward index scan | 69ms (**약 1ms**) |
| 오프셋: `ORDER BY id DESC LIMIT 500000, 16` | `index`, **500,016행 스캔** | 168ms (**약 100ms**) |
| `WHERE status='ON_SALE' ORDER BY created_at DESC LIMIT 16` | **`ALL` 풀스캔 + `Using filesort`**, 798,124행 | 738ms (**약 670ms**) |
| `SELECT COUNT(*) FROM product_likes WHERE product_id = ?` | `ref` + **`Using index`**(커버링) | 66ms (**약 0ms**) |

읽어낼 것:

1. **`status` + 최신순은 인덱스가 아예 없다** — 풀스캔에 filesort까지 붙어 670ms. 축 3(PERF-3)의 1순위 대상이며, 복합 인덱스 `(status, created_at)`으로 개선하는 이야기가 여기서 시작된다.
2. **커서 대비 오프셋이 약 100배 느리다** — PERF-6의 실측 근거 확보. 이미 커서 페이지네이션을 쓰고 있다는 선택의 값이 숫자로 나왔다.
3. **찜수 조회는 이미 커버링 인덱스로 해결돼 있다** — `idx_product_likes_product` 추가(DTL-2 코드리뷰)가 지금 규모에서도 유효함을 확인.

## 6. 알려진 한계

- **`Order`에 `createdAt`이 없다.** 주문에 시간 분포를 만들 수 없어 기간 필터·정렬 실험은 상품 기준으로만 가능하다. 방치 주문 정리 스케줄러(INTG-5)도 기준 시각이 없어 막힌다 → 별도 작업 필요.
- **컨테이너를 끄면 데이터가 사라진다.** Testcontainers가 JVM 종료 시 컨테이너를 제거하기 때문이다. 매 기동마다 재시딩하는 방식을 택했다(약 8분). 재시딩이 부담이 되면 `withReuse(true)` + `~/.testcontainers.properties`로 컨테이너를 유지할 수 있으나, 그 경우 **`ddl-auto: update`가 지운 인덱스를 DB에서 제거하지 않아** 실험이 오염될 수 있다. 인덱스는 SQL로 직접 관리하는 편이 안전하다.
- **`product_tag`에 유니크 제약이 없다.** Hibernate가 `List` 기반 `@ManyToMany`에는 PK·유니크를 만들지 않는다. 시더는 상품 내 태그 중복이 없도록 생성하지만 **DB가 보장하지는 않는다.**
- 시딩 중 `FOREIGN_KEY_CHECKS=0`을 쓴다. 세션 변수라 **커넥션이 바뀌면 무효**가 되므로 시딩 전체를 커넥션 하나에 묶었다(`BulkInserter`).

## 7. 다음

- **OPS-2a k6** — 목록/상세/구매 3종 시나리오, p95·p99·TPS. 시드 계정(`seed{n}@ibid.test` / `pass1234`)으로 로그인 가능.
- **PERF-3** — `(status, created_at)` 복합 인덱스로 위 670ms를 개선하고 `EXPLAIN` before/after 기록.
- **PERF-6** — 커서 vs 오프셋 비교를 부하 조건에서 재측정.

---

*작성 2026-08-11 · 상품 200만행 기준.*
