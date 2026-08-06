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

### Phase 1 — 상호작용
- [x] **DTL-1 공유** — 링크 복사 / Web Share (FE)
- [~] **DTL-2 찜/좋아요** — 진행 중 (`feat/product-like`)
  - `ProductLike`(DB, unique 유저·상품) · 찜(POST)/취소(DELETE) · 찜 상태(GET `{id}/like`) · 관심목록(`me/likes`) · `/likes` 페이지 · 상세 하트 토글
  - Redis 찜수 캐시는 **제외** — `COUNT`+인덱스로 충분(조기 최적화), 부하 시 재검토
  - ⚠️ 접점: 상세 **메타행**이 #34 조회수와 프론트에서 겹침(백엔드는 분리 엔드포인트라 충돌 없음) → 머지 시 `👁 조회수` 옆 `❤ 찜수` 합치기

### Phase 2 — 상품 데이터·미디어
- [x] **DTL-4 상품 필드확장** — `tags`(`Tag` 엔티티 `@ManyToMany`·재사용, 최대 10개·상세 표시) · `createdAt`(상대시간) · **`shippingFee`(0원=무료배송, 플랫폼 관리)** — 등록/수정 폼·상세 연동. 태그 모델 결정 근거는 [`FEATURE_SPEC.md`](./FEATURE_SPEC.md#23-상품-product) (BE+FE)
- [x] ★ **DTL-5 이미지 S3** — presigned 업로드(다중)·상세 갤러리·홈 썸네일 (BE+FE)
- [x] **DTL-5+ 상품 수정/삭제** — 수정 페이지·삭제 버튼, 이미지 추가/개별삭제·상품삭제 시 **S3 동기 삭제** (BE+FE)

### Phase 3 — 알림 발행
- [ ] **NOTI-1** `OrderPaid` 이벤트 발행 — 결제확정(`OrderService.confirmPaid`) 시 (BE)

**권장 순서**: ~~DTL-5 이미지~~ ✅ → ~~DTL-1 공유~~ ✅ → DTL-2 찜 → ~~DTL-4 필드확장~~ ✅ → NOTI-1

---

## 🧑‍💻 김진호 (협업자) — 참고
- **DTL-3 조회수**(Redis 카운터) · **DTL-6 판매자 상점/통계** · **DTL-7 유사 추천** · **NOTI-2~4 알림**(도메인·API·프론트)

---

## 🔗 공유 접점 & 주의 (먼저 합의)
- **찜(장동현) ↔ 조회수(김진호)**: `ProductDetailResponse`·상세 메타행·`ProductController`를 함께 건드림 → **응답 필드·메타행 구조 먼저 합의**, Redis 키 컨벤션(`product:{id}:likes`/`:views`)·카운트 `INCR`
- **알림 이벤트 계약**: `OrderPaid(orderId, sellerId)` — 장동현 발행 → 김진호 구독
- **배송비(`shippingFee`)**: 결제 총액 합산 + 플랫폼 수취(판매자 정산 미포함) → 결제/정산과 정합 확인

## ⏭ 이후 (기술 성장 — BACKLOG 참고)
- 대용량 시딩·인덱스·검색(ES)·캐싱·부하테스트(k6)·관측성·AWS 배포
