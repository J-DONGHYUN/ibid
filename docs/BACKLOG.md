# 개발 백로그 — 현재 작업 (상품 상세 완성)

> **방향**: 일반 중고거래 + **검수 에스크로 유지** ([`PRODUCT_PLAN.md`](./PRODUCT_PLAN.md)) · 전 거래 배송 기반(직거래 없음).
> **삭제 확정**: 직거래 · 채팅 · 사이즈. **배송비 = 플랫폼이 수취·관리**(판매자 정산 미포함).
> **목표**: 기술 성장(성능·인프라) — 찜/조회수(Redis)·이미지(S3) 등이 그 소재.

## ✅ 완료
- STAB-1(재고 부풀림) · STAB-6(RBAC) · 재고 동시성 락 · 결제(토스, **종료**)
- **상품상태 `condition` enum**(NEW/LIKE_NEW/USED) — 등록폼·상세 연동
- **직거래 · 채팅 · 사이즈 삭제** (상세/등록 화면)

## 🎯 현재 작업

### 상품 상세 페이지 실동작화
지금 화면은 UI만 있고 데이터가 하드코딩 → 실제 백엔드 연동으로 전환.

| ID | 작업 | FE/BE | 기술포인트 |
| --- | --- | --- | --- |
| DTL-1 | **공유** 버튼 (링크 복사 / Web Share) | FE | - |
| DTL-2 | **찜/좋아요** — `POST/DELETE /api/products/{id}/like` + 찜수 + 내 찜여부 | BE+FE | Redis 찜수·집계 |
| DTL-3 | **조회수** — 조회 시 증가 + 표시 | BE+FE | **Redis 카운터**(성능) |
| DTL-4 | **상품 필드 확장** — `tags` · `createdAt`(등록시각) · `shippingFee`(플랫폼 관리) | BE+FE | 도메인 모델링 |
| DTL-5 | **상품 이미지 업로드(다중)** + 갤러리 | BE+FE | 파일·S3 |
| DTL-6 | **판매자 상점/통계** — 판매중 수·거래 횟수 + 상점 페이지 | BE+FE | 집계 |
| DTL-7 | **유사 상품 추천** — 같은 카테고리/태그 | BE+FE | 추천 |
| DTL-8 | (선택) **신고** 접수 | BE+FE | - |

**권장 순서**: DTL-1(공유) → DTL-2(찜) → DTL-3(조회수) → DTL-4(필드) → DTL-5(이미지) → DTL-6/7(상점·추천)

### 알림 (거래 상태 기반)

이벤트 기반(Spring `ApplicationEvent` + `@TransactionalEventListener(AFTER_COMMIT)`, 검수 이벤트와 동일 패턴). 메시지 큐 미사용(이메일/푸시 붙으면 Kafka로 승격 → "이후").

| ID | 작업 | FE/BE | 기술포인트 |
| --- | --- | --- | --- |
| NOTI-1 | `OrderPaid` 이벤트 발행 — 결제확정 시 `(orderId, sellerId)` | BE | 이벤트 발행 |
| NOTI-2 | 알림 도메인(`Notification`) + 구독자 → **판매자 "결제완료, 발송하세요" 알림** 생성 | BE | 이벤트 구독·도메인 |
| NOTI-3 | 알림 조회/읽음 API — `GET /api/notifications` · `PATCH /{id}/read` · 미읽음 수 | BE | 조회·상태 |
| NOTI-4 | 알림 프론트 — `/notifications` 목록 + 헤더 미읽음 뱃지 | FE | UI |

> 접점(합의): 알림 조회 API 스펙 + `OrderPaid` 이벤트 계약. Defer: 실시간(SSE/푸시)·이메일·알림 설정·다른 알림 종류.

### 분담

**장동현**

| ID | 기능 | FE/BE |
| --- | --- | --- |
| DTL-1 | 공유 | FE |
| DTL-2 | 찜/좋아요 | BE+FE |
| DTL-4 | 상품 필드확장 (tags·createdAt·shippingFee) | BE+FE |
| DTL-5 | 이미지 S3 | BE+FE |
| NOTI-1 | `OrderPaid` 이벤트 발행 | BE |

**김진호**

| ID | 기능 | FE/BE |
| --- | --- | --- |
| DTL-3 | 조회수 | BE+FE |
| DTL-6 | 판매자 상점/통계 | BE+FE |
| DTL-7 | 유사 상품 추천 | BE+FE |
| NOTI-2~4 | 알림 도메인·API·프론트 | BE+FE |

**⚠️ 주의 (찜·조회수 충돌)**
- 같은 파일(`ProductDetailResponse` · 상세 메타행 · `ProductController`)을 함께 건드림 → **응답 필드·메타행 구조 먼저 합의**
- Redis: 설정은 한 명이 · 키 컨벤션(`product:{id}:likes` / `:views`) · 카운트는 **`INCR`(원자적)**

---

## ⏭ 이후 (기술 성장 로드맵)
> 상세 완성 후 착수. 목표(성능·인프라)의 본체.
- 대용량 더미 시딩 · N+1/인덱스 튜닝 · **검색(ES)** · 캐싱 · **부하테스트(k6)+관측성** · **AWS 배포/CI**

## 🚫 범위 밖
- 직거래 · 채팅 · 사이즈 · 결제 정합성 심화 · 커뮤니티(게시판/쿠폰) · 배송사 연동 · 경매

*작성 2026-07 · 일반 중고 + 검수 에스크로, 상품 상세 완성 중심.*
