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

**의존성 방향(확정)**: `payment → order`. 결제가 주문을 호출/구독하고, **order·inspection은 payment를 모른다**([#15](https://github.com/J-DONGHYUN/ibid/issues/15), [FEATURE_SPEC §2.6](./FEATURE_SPEC.md#26-결제-연동-경계)).
- 구매 오케스트레이션(checkout/confirm 엔드포인트) = **결제 모듈(팀원)**
- order는 결제가 호출할 유스케이스(`purchase`/`confirmPaid`/`cancel`)만 노출
- 정산/환불 = inspection이 **이벤트 발행** → 결제가 구독

### 선행 (완료)

- [x] **T0. 상품 판매 시작 흐름 연결** — `PATCH /api/products/{id}/on-sale`(`PENDING`→`ON_SALE`)

### #15 연동 — 주문 쪽 필수 수정 (결제팀 요청)

- [x] **O1. `OrderService.purchase()` 반환값 변경** — `Long orderId` → `(orderId, totalPrice)` 결과 객체 (결제가 order repo 직접 접근 안 하도록).
- [ ] **O2. `Product.increaseStock(quantity)` 추가** — 재고 복원용(현재 `decreaseStock`만 존재).
- [ ] **O3. 주문 취소 유스케이스** — `OrderService.cancel(orderId)` → 재고 복원 + 주문 `CANCELED` (결제 실패/이탈 보상).
- [ ] **O4. 결제 확정 반영** — `OrderService.confirmPaid(orderId)` → 주문 `CREATED` → `PAID`.
- [ ] **O5. 경계 유지** — order/inspection이 payment/Toss를 import하지 않음 확인.

### 주문 flow (본인 핵심)

- [ ] **T3. OrderStatus 상태기계 도입** — `CREATED / PAID / SHIPPED_TO_INSPECTOR / UNDER_INSPECTION / COMPLETED / REFUNDED / CANCELED` + 잘못된 전이 방지(`409`).
- [ ] **T5. 판매자 발송 처리** — `POST /api/orders/{id}/ship`(판매자 본인). `PAID` → `SHIPPED_TO_INSPECTOR`.
- [ ] **T6. 내 거래 목록/상세** — 구매자·판매자 관점 주문 조회.
- [ ] **T7. `CREATED` 방치 주문 타임아웃 정리(후순위)** — 결제 미완 주문 자동 취소/재고 복원.

### 검수 (inspection 도메인 — 본인)

- [ ] **T8. Inspection 엔티티 + 도메인** — 주문당 검수 기록, 상태 `WAITING/RECEIVED/PASSED/FAILED`.
- [ ] **T9. 검수 수령 처리** — `POST /api/inspections/{orderId}/receive`(운영자). `SHIPPED_TO_INSPECTOR` → `UNDER_INSPECTION`.
- [ ] **T10. 검수 통과 → 정산 이벤트** — `POST /api/inspections/{orderId}/pass`. `InspectionPassed` 발행(결제 구독), 주문 `COMPLETED`.
- [ ] **T11. 검수 불합격 → 환불 이벤트** — `POST /api/inspections/{orderId}/fail`. `InspectionFailed` 발행(결제 구독), 주문 `REFUNDED`.
- [ ] **T12. 운영자 권한 가드** — 검수 API는 `ROLE_ADMIN`만 접근.

### 합의 필요 (팀원과)

- [ ] **A1. 검수 이벤트 계약** — `InspectionPassed`/`InspectionFailed` payload와 결제 구독 처리, 실패 재시도 정책.
- [ ] **A2. 판매자 정산 수단** — 검수 통과 시 실제 판매자 지급 방식(범위/시점).

### 마무리

- [ ] **T13. 통합 테스트** — 판매시작→(결제)→발송→검수 통과/불합격 시나리오. order는 payment 없이 검증(유스케이스/상태기계 단위).
- [ ] **T14. 프론트 연동** — 내 거래 상태 화면, (운영자) 검수 처리 화면. (구매/결제창은 결제팀 checkout 응답 사용)
- [ ] **T15. 문서 갱신** — 기능 추가 시 `FEATURE_SPEC.md`·`CLAUDE.md` 최신 상태 유지.

### 권장 진행 순서

```
T0(완료) → O1·O2·O3·O4(#15 연동) → T3(상태기계) → T5(발송) → T8·T9(검수) → T10·T11(정산/환불 이벤트) → T12(권한) → T6·T13(조회/통합) → T14·T15
```

병행: **A1·A2(이벤트 계약·정산 수단)** 는 T10/T11 착수 전까지 팀원과 합의.
