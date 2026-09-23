# ADR-0001 도메인 패키지 + 4계층

- 상태: 채택
- 관련: `03-convention/architecture.md`

## 맥락

기능을 추가할 때 코드가 어디에 들어가야 하는지가 매번 판단의 대상이 되면, 사람마다
(에이전트마다) 다른 곳에 둔다. 도메인을 지우거나 새로 만들 때 영향 범위도 보이지 않는다.

## 결정

최상위를 **도메인**(`auth` · `user` · `product` · `chat` · `trade` · `notification` · `common`)으로
나누고, 각 도메인 안을 `presentation` · `application` · `domain` · `infra` 4계층으로 나눈다.

## 대안

- **계층 우선** (`controller/` · `service/` · `repository/` 최상위) — 도메인 하나를 지우려면
  네 폴더를 뒤져야 한다. 이번 방향 전환에서 `order` · `payment` · `inspection` 을 통째로
  지우는 것이 도메인 우선이라 가능하다
- **헥사고날 (포트 · 어댑터)** — 도메인에 포트 인터페이스를 두고 infra 에서 구현한다. 얻는 것은
  기술 교체의 격리 하나인데, 혼자 하는 프로젝트에서 인터페이스 · 구현 쌍을 계속 유지하는
  비용이 더 크다

## 결과

- 저장소 인터페이스는 `infra` 에 두고 Spring Data 를 상속한다
- 계층 간 · 도메인 간 의존 규칙은 `architecture.md` 에 있고, 게이트로 검사한다
