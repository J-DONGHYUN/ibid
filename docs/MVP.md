# MVP 로드맵 — 역할 분담

> 전체 백로그·주의 → [`BACKLOG.md`](./BACKLOG.md) · 제품 방향 → [`PRODUCT_PLAN.md`](./PRODUCT_PLAN.md)
> 방향: **일반 중고거래 + 검수 에스크로**(전 거래 배송, 직거래 없음). 삭제: 직거래·채팅·사이즈.
> 진행: 이슈 → 브랜치 → PR(`Closes #N`) · 완료 조건: **테스트 포함** · 상태: `[ ]`/`[~]`/`[x]`

## 완료
- [x] MVP1·2, STAB-1(재고 부풀림), STAB-6(**RBAC**)
- [x] **상품상태 `condition` enum**(NEW/LIKE_NEW/USED) — 등록/상세 연동
- [x] **직거래·채팅·사이즈 삭제**

---

## 🧑‍💻 장동현 (본인)

### 한눈에 보기

| ID | 작업 | 상태 | PR |
| --- | --- | --- | --- |
| DTL-1 | 공유 | ✅ 완료 | #32 |
| DTL-5 | 이미지 S3 (+수정/삭제) | ✅ 완료 | #32 |
| DTL-2 | 찜/좋아요 | 🔵 리뷰 중 | **#35** |
| DTL-4 | 상품 필드확장 | 🟡 로컬 완료, PR 미생성 | - (`feat/product-fields`) |
| NOTI-1 | `OrderPaid` 이벤트 발행 | ⬜ 예정 | - |

### Phase 1 — 상호작용
- [x] **DTL-1 공유** — 링크 복사 / Web Share (FE)
- [x] **DTL-2 찜/좋아요** — 구현 완료, PR [#35](https://github.com/J-DONGHYUN/ibid/pull/35) 리뷰 중 (`feat/product-like`)
  - `ProductLike`(독립 애그리거트, id 참조, unique 유저·상품) · 찜(POST)/취소(DELETE) · 찜 상태(GET `{id}/like`) · 관심목록(`me/likes`) · `/likes` 페이지 · 상세 하트 토글
  - Redis 찜수 캐시는 **제외** — `COUNT`+인덱스로 충분(조기 최적화), 부하 시 재검토
  - **코드리뷰 반영**(설계 근거는 [`FEATURE_SPEC.md §2.3`](./FEATURE_SPEC.md#찜좋아요--dtl-2))
    - 찜수 조회용 `idx_product_likes_product` 인덱스 추가 — 유니크 인덱스가 `(user_id, product_id)` 순이라 `product_id` 단독 조회에 못 쓰여 풀스캔이었음(`EXPLAIN` 9943행 → 20행)
    - 동시 찜 시 유니크 제약 위반이 **500 → 409**(`DataIntegrityViolationException` 전역 핸들러). 서비스는 여전히 `throw`만 함
    - 프론트 하트 in-flight 가드 + 409 시 찜 상태 재조회
    - 동시성 통합 테스트(30스레드) + 409 응답 컨트롤러 테스트 추가
  - ⚠️ 접점: 상세 **메타행**이 [#34](https://github.com/J-DONGHYUN/ibid/pull/34) 조회수와 프론트에서 겹침 → 머지 시 `👁 조회수` 옆 `❤ 찜수` 합치기
  - ⚠️ **#34 머지 후 확인 필요**: #34가 상품 목록·상세를 `@PublicApi`로 열어 비로그인 진입이 가능해지는데, 찜 상태 조회(`GET {id}/like`)는 로그인 필수라 **비로그인 사용자에겐 401 → 찜수가 0으로 보인다** → 아래 백로그 `DTL-2+` 참고

### Phase 2 — 상품 데이터·미디어
- [x] ★ **DTL-5 이미지 S3** — presigned 업로드(다중)·상세 갤러리·홈 썸네일 (BE+FE)
- [x] **DTL-5+ 상품 수정/삭제** — 수정 페이지·삭제 버튼, 이미지 추가/개별삭제·상품삭제 시 **S3 동기 삭제** (BE+FE)
- [~] **DTL-4 상품 필드확장** — 구현 완료, **로컬 브랜치 `feat/product-fields`에만 있음 (PR 미생성)** (BE+FE)
  - `tags` — `Tag` 엔티티 `@ManyToMany`(조인 테이블 `product_tag`), 같은 이름 태그는 find-or-create로 **재사용**. 모델 선택 근거는 [`FEATURE_SPEC.md`](./FEATURE_SPEC.md#23-상품-product)
  - `createdAt` — 등록시각, 상세에서 상대시간 표시
  - `shippingFee` — 0원=무료배송, 등록/수정 폼·상세 연동
  - ⚠️ **미해결 리뷰 지적 있음** → BACKLOG `DTL-4+` 참고. 특히 **`shippingFee`가 결제 총액에 합산되지 않음**
  - `feat/product-like`(#35)를 **로컬 병합 완료** — #35가 main에 머지된 뒤 PR 생성 예정

### Phase 3 — 알림 발행
- [ ] **NOTI-1** `OrderPaid` 이벤트 발행 — 결제확정(`OrderService.confirmPaid`) 시 (BE)

**권장 순서**: ~~DTL-5 이미지~~ ✅ → ~~DTL-1 공유~~ ✅ → ~~DTL-2 찜~~ ✅(리뷰 중) → **DTL-4 마무리·PR** → NOTI-1

---

## 🧑‍💻 김진호 (협업자) — 참고
- **DTL-3 조회수**(Redis 카운터, PR [#34](https://github.com/J-DONGHYUN/ibid/pull/34)) · **DTL-7 유사 추천** · **NOTI-2~4 알림**(도메인·API·프론트)
- ❌ **DTL-6 판매자 상점/통계 — 제외 결정** ([#36](https://github.com/J-DONGHYUN/ibid/issues/36)): 마이페이지와 중복 + 검수 에스크로가 판매자 신뢰를 대체. 담당 배정돼 있던 항목이라 이슈에서 합의 진행 중

---

## 🔗 공유 접점 & 주의 (먼저 합의)
- **찜(장동현) ↔ 조회수(김진호)**: `ProductDetailResponse`·상세 메타행·`ProductController`를 함께 건드림 → **응답 필드·메타행 구조 먼저 합의**, Redis 키 컨벤션(`product:{id}:likes`/`:views`)·카운트 `INCR`
- **알림 이벤트 계약**: `OrderPaid(orderId, sellerId)` — 장동현 발행 → 김진호 구독
- **배송비(`shippingFee`)**: 결제 총액 합산 + 플랫폼 수취(판매자 정산 미포함) → 결제/정산과 정합 확인

## ⏭ 이후 (기술 성장 — BACKLOG 참고)
- 대용량 시딩·인덱스·검색(ES)·캐싱·부하테스트(k6)·관측성·AWS 배포
