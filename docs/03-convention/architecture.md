# 아키텍처 컨벤션

패키지 · 계층 · 의존 방향을 정한다. 근거는 `ADR-0001` · `ADR-0006`.

**이 문서의 `[G]` 규칙은 전부 게이트로 옮길 대상이다.** 규칙마다 적힌 `현재 위반` 은
2026-09-23 기준으로 센 값이고, 게이트를 켜기 전에 0으로 만든다.

## 도메인과 의존 방향

```
                 trade
                /     \
               ▼       ▼
             chat ───▶ product
               \       /
                ▼     ▼
                 auth ───▶ user
                   │
                   ▼
                 common           notification  ← 누구도 import 하지 않는다 (이벤트로만 받는다)
```

| 도메인 | 하는 일 | 의존해도 되는 도메인 |
|---|---|---|
| `common` | 예외 체계 · 설정 · 이미지 저장(`common.image`) | 없음 |
| `user` | 사용자 | `common` |
| `auth` | 가입 · 로그인 · 토큰 · 인증 표시(`@PublicApi` · `@LoginUser`) | `user` · `common` |
| `product` | 상품 · 이미지 · 찜 · 상품 상태 전이 | `auth` · `common` |
| `chat` | 채팅방 · 메시지 · 읽음 | `product` · `auth` · `common` |
| `trade` | 예약 · 거래완료 조율 · 판매/구매 내역 | `chat` · `product` · `auth` · `common` |
| `notification` | 알림 저장 · 조회 | `auth` · `common` |

- [G] 표에 없는 방향의 import 를 하지 않는다. 특히 **`product` → `chat` · `trade`, `chat` → `trade` 금지** (`ADR-0006`)
- [G] 어떤 도메인도 `notification` 을 import 하지 않는다. 알림은 이벤트로만 받는다 (`ADR-0009`)
- [G] `common.image` 는 어떤 도메인도 import 하지 않는다. 이미지를 소유하는 쪽은 도메인이다
  (`product.ProductImage` 가 `common.image` 의 업로더를 쓴다)
- [G] **삭제 예정 도메인**(`order` · `payment` · `inspection`)을 새 코드가 import 하지 않는다.
  기존 코드를 고칠 때도 이 도메인을 확장하지 않는다 (`PRD.md` 현재 코드와의 차이)

## 계층

| 계층 | 두는 것 | 의존해도 되는 계층 |
|---|---|---|
| `presentation` | 컨트롤러 · 요청/응답 DTO · 쿠키 · 인터셉터 | `application` · `domain` (응답 조립용 읽기) |
| `application` | 유스케이스 서비스 · 커맨드 · 트랜잭션 경계 | `domain` · `infra` |
| `domain` | 엔티티 · 값 객체 · 상태 전이 · 도메인 규칙 | 없음 (`common.exception` · `jakarta.persistence` 만) |
| `infra` | Spring Data 저장소 · Redis · 외부 클라이언트 | `domain` |

- [G] `application` 은 `presentation` 을 import 하지 않는다. 요청 DTO 를 받지 말고 커맨드로 받는다
  — **현재 위반 10개 파일** (유지할 도메인: `AuthService` · `ProductService` · `ProductImageService` ·
  `ProductLikeService` / 나머지 6개는 삭제 예정 도메인)
- [G] `domain` 은 Spring 을 import 하지 않는다 — 현재 위반 0
- [G] `domain` 은 같은 도메인의 `application` · `infra` · `presentation` 을 import 하지 않는다 — 현재 위반 0
- [G] `@Transactional` 은 `application` 에만 둔다 — **현재 위반 1개** (`ProductRepository`)
- [G] 저장소 인터페이스는 `infra` 에 둔다 (`ADR-0001`) — 현재 위반 0

## 코드가 어디에 들어가나

| 이런 코드 | 여기 |
|---|---|
| "예약중이면 다시 예약할 수 없다" 같은 상태 규칙 | `domain` 엔티티의 메서드 |
| 두 애그리게이트를 봐야 하는 판정 ("예약 상대는 채팅 상대다") | 둘을 모두 볼 수 있는 도메인의 `application` (`trade`) |
| 요청 형식 검사 (빈 값 · 길이) | `presentation` DTO 의 Bean Validation. **최종 방어는 도메인이 한다** |
| 복잡한 조회 쿼리 | `infra` 저장소 메서드 |
| 외부 호출(S3 · Kafka) | `infra`. `application` 은 그 결과만 받는다 |
