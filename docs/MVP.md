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
- [x] **O2. `Product.restoreStock(quantity)` 추가** — 재고 복원용(`decreaseStock`의 역동작, PENDING 복원 차단).
- [x] **O3. 주문 취소 유스케이스** — `OrderService.cancel(orderId)` → 재고 복원 + 주문 `CANCELED` (결제 실패/이탈 보상). *(OrderStatus enum·status 필드 최소 도입 포함)*
- [x] **O4. 결제 확정 반영** — `OrderService.confirmPaid(orderId)` → 주문 `CREATED` → `PAID`.
- [x] **O5. 경계 유지** — order가 payment/Toss를 import하지 않음 확인(현재 `auth`·`product`만 의존). inspection은 T8에서 동일 원칙 적용.

### 주문 flow (본인 핵심)

- [x] **T3. OrderStatus 상태기계 도입** — 전 전이 완료: `CANCELED`(O3)·`PAID`(O4)·`SHIPPED_TO_INSPECTOR`(T5)·`UNDER_INSPECTION`(T9)·`COMPLETED`(T10)·`REFUNDED`(T11). 각 전이 가드로 잘못된 전이 `409`.
- [x] **T5. 판매자 발송 처리** — `POST /api/orders/{id}/ship`(판매자 본인). `PAID` → `SHIPPED_TO_INSPECTOR`.
- [x] **T6. 내 거래 목록/상세** — `GET /api/orders?role=buyer|seller`(목록, 상품명 배치 조회), `GET /api/orders/{id}`(상세, 당사자만). 목록은 ID 참조라 `findAllById` 배치로 상품명 매핑.
- [ ] **T7. `CREATED` 방치 주문 타임아웃 정리(후순위)** — 결제 미완 주문 자동 취소/재고 복원.

### 검수 (inspection 도메인 — 본인)

- [x] **T8. Inspection 판정 기록 도메인 (옵션 C)** — 상태 흐름은 **Order가 소유**, Inspection은 판정 결과 기록(`orderId/inspectorId/result(PASSED|FAILED)/memo`). 자체 상태기계 없음. InspectionRepository 포함.
- [x] **T9. 검수 수령 처리** — `POST /api/inspections/{orderId}/receive`. 주문 전이 `SHIPPED_TO_INSPECTOR` → `UNDER_INSPECTION`(Inspection 기록 없음). InspectionService(검수 유스케이스의 집) 도입, order→startInspection 위임. *(권한 ROLE_ADMIN은 T12)*
- [x] **T10. 검수 통과 → 정산 이벤트** — `POST /api/inspections/{orderId}/pass`. `Inspection.passed` 기록 + 주문 `COMPLETED` + `InspectionPassed(orderId)` 발행(결제 구독). *이벤트 payload는 A1 확정 전 잠정(orderId).*
- [x] **T11. 검수 불합격 → 환불 이벤트** — `POST /api/inspections/{orderId}/fail`. `Inspection.failed` 기록 + 주문 `REFUNDED` + **재고 복원** + `InspectionFailed(orderId)` 발행(결제 구독). *payload는 A1 확정 전 잠정.*
- [ ] **T12. 운영자 권한 가드 → MVP3로 연기** — 검수 API를 `ADMIN`만 접근. 로컬 단독 테스트 단계라 지금은 보류(모두 로그인 사용자면 호출 가능). ⚠️ **배포 전 필수** — role을 JWT에 싣는 작업과 함께 MVP3에서 처리.

### 합의 필요 (팀원과)

- [ ] **A1. 검수 이벤트 계약** — `InspectionPassed`/`InspectionFailed` payload와 결제 구독 처리, 실패 재시도 정책.
- [ ] **A2. 판매자 정산 수단** — 검수 통과 시 실제 판매자 지급 방식(범위/시점).

### 마무리

- [x] **T13. 통합 테스트** — `TradeScenarioIntegrationTest`: 구매→결제확정→발송→수령→통과(`COMPLETED`)/불합격(`REFUNDED`+재고복원) 전 사이클 + 잘못된 전이 차단을 실제 DB(Testcontainers)로 검증. 결제 없이 order/inspection 서비스로 완주.
- [ ] **T14. 프론트 연동** — 내 거래 상태 화면, (운영자) 검수 처리 화면. (구매/결제창은 결제팀 checkout 응답 사용)
  - **와이어프레임 (그릴 목록)** — 이미지 확정 후 F1(배선)→화면 순 구현

    | # | 화면 | 경로(제안) | 액터 | 신규/기존 |
    |---|---|---|---|---|
    | A1 | 내 거래 목록(마이페이지) — 구매/판매 탭 + 상태 뱃지 | `/mypage` | 공용 | 신규(현재 빈 껍데기) |
    | A2 | 거래 상세 — 상태 스텝퍼 + 상태별 액션 | `/orders/[id]` | 공용 | 신규 |
    | B1 | 구매(결제) 모달 — 수량·총액·구매 (결제창 자리) | 상품상세 내 | 구매자 | 기존 `PurchaseModal` 확정 |
    | C1 | 상품 상세 — 본인·`PENDING`이면 "판매 시작" | `/products/[id]` | 판매자 | 기존 확장 |
    | C2 | 발송 처리 — `PAID` 주문 발송(A2 내 액션/모달) | A2 내 | 판매자 | 신규 |
    | D1 | 검수 처리 목록 — 수령/통과/불합격 + 판정 메모 | `/admin/inspections` | 운영자 | 신규 |
    | E1 | 주문 상태 뱃지/스텝퍼 (7상태) 공통 컴포넌트 | - | 공통 | 신규 |

    - 핵심 3개(사이클 완성): **A1 · A2 · D1**. B1/C1은 기존 확정 수준.
    - ⚠️ B1 결제창은 결제팀 checkout 규격 확정 후. 그 전까진 "구매=주문 생성(CREATED)"까지.
- [ ] **T15. 문서 갱신** — 기능 추가 시 `FEATURE_SPEC.md`·`CLAUDE.md` 최신 상태 유지.

### 권장 진행 순서

```
T0(완료) → O1·O2·O3·O4(#15 연동) → T3(상태기계) → T5(발송) → T8·T9(검수) → T10·T11(정산/환불 이벤트) → T6·T13(조회/통합) → T14·T15
```

- 병행: **A1·A2(이벤트 계약·정산 수단)** 는 T10/T11 착수 전까지 팀원과 합의.
- **MVP3로 연기**: T12(운영자 권한, 배포 전 필수), T7(방치 주문 타임아웃, 후순위).
