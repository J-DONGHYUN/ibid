# MVP 로드맵 / 할 일

MVP 단계별 작업 목록과 진행 상태를 관리하는 문서다. **무엇을 만들 것인가(기능 정의·플로우)** 는 [FEATURE_SPEC.md](./FEATURE_SPEC.md)에 있고, 이 문서는 **무엇을 언제 할 것인가(작업·진행)** 만 다룬다.

- 진행 흐름: 이슈(`enhancement` 라벨) → 브랜치 → PR(`Closes #N`)
- 완료 조건: 각 항목은 **테스트 코드 포함**이 기본 (see [TEST.md](./TEST.md))
- 상태 표기: `[ ]` 대기 / `[~]` 진행 중 / `[x]` 완료

---

## MVP 1차 (완료)

- [x] 회원가입/로그인/로그아웃/토큰 재발급 (JWT + Redis 리프레시 토큰)
- [x] 상품 등록 / 목록(커서 페이지네이션) / 상세
- [x] 상품 구매(재고 차감, 비관적 락 동시성 제어, 본인 거래 차단)

> 상세 명세는 [FEATURE_SPEC.md §2](./FEATURE_SPEC.md#2-도메인별-기능).

---

## MVP 2차 (주문 flow + 검수 에스크로)

담당 경계: **주문 flow = 본인**, **결제(에스크로 3단계·외부 PG) = 팀원**. 자세한 내용은 [FEATURE_SPEC.md §3.1](./FEATURE_SPEC.md#31-담당-경계).

### 선행 (착수 전 필수)

- [ ] **T0. 상품 판매 시작 흐름 연결** — 등록 상품이 `PENDING`에 머물러 구매 불가한 1차 구멍 해결. 판매 시작 API(`PATCH /api/products/{id}/on-sale`) 추가 또는 등록 즉시 `ON_SALE`.

### 결제 경계 (팀원과 합의 → 병렬 개발 시작점)

- [ ] **T1. 결제 포트 인터페이스 확정** — `PaymentPort`(hold/settle/refund) 시그니처·에러·`paymentId` 계약을 팀원과 합의하고 인터페이스로 고정.
- [ ] **T2. 결제 포트 스텁 구현** — 실제 PG 없이 성공/지정 실패하는 스텁으로 주문 flow를 먼저 완성.

### 주문 flow (본인 핵심)

- [ ] **T3. OrderStatus 상태기계 도입** — `PAYMENT_HELD / SHIPPED_TO_INSPECTOR / UNDER_INSPECTION / COMPLETED / REFUNDED` + 잘못된 전이 방지 + `paymentId` 보관.
- [ ] **T4. 구매 시 결제 hold 연동** — 재고 차감 + `hold` 호출 + 주문 `PAYMENT_HELD` 생성. `hold` 실패 시 주문 미생성/재고 원복.
- [ ] **T5. 판매자 발송 처리** — `POST /api/orders/{id}/ship`(판매자 본인). 상태 → `SHIPPED_TO_INSPECTOR`.
- [ ] **T6. 내 거래 목록/상세** — 구매자·판매자 관점 주문 조회.
- [ ] **T7. 미발송 기한초과 자동 취소(후순위)** — `PAYMENT_HELD` 장기 미발송 시 `refund` → `REFUNDED`.

### 검수 (inspection 도메인 — 본인)

- [ ] **T8. Inspection 엔티티 + 도메인** — 주문당 검수 기록, 상태 `WAITING/RECEIVED/PASSED/FAILED`.
- [ ] **T9. 검수 수령 처리** — `POST /api/inspections/{orderId}/receive`(운영자). 상태 → `UNDER_INSPECTION`.
- [ ] **T10. 검수 통과 → 정산** — `POST /api/inspections/{orderId}/pass`. `settle` 호출, 주문 `COMPLETED`.
- [ ] **T11. 검수 불합격 → 환불** — `POST /api/inspections/{orderId}/fail`. `refund` 호출, 주문 `REFUNDED`.
- [ ] **T12. 운영자 권한 가드** — 검수 API는 `ROLE_ADMIN`만 접근.

### 마무리

- [ ] **T13. 통합 테스트** — 등록→판매시작→구매(hold)→발송→검수 통과/불합격 전체 시나리오 회귀 테스트(스텁 결제 기준).
- [ ] **T14. 프론트 연동** — 구매 버튼, 내 거래 상태 화면, (운영자) 검수 처리 화면.
- [ ] **T15. 문서 갱신** — 기능 추가 시 `FEATURE_SPEC.md`·`CLAUDE.md` 최신 상태 유지.

### 권장 진행 순서

```
T0 → T1/T2(결제 경계 합의+스텁) → T3 → T4 → T5 → T8/T9 → T10/T11 → T12 → T6/T13 → T14 → T15
```

판매 시작을 열고 → 결제 경계를 스텁으로 확보 → 상태기계와 구매 → 발송 → 검수/권한 → 조회/프론트 순.
