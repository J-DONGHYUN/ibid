# ibid

**동네에서 만나 거래하는 중고 전자기기 마켓의 API 서버**입니다.

그리고 이 저장소의 진짜 주제는 **AI 에이전트와 개발하는 방식**입니다.
에이전트가 매번 같은 실수를 하지 않도록 **컨텍스트 · 하네스 · 루프**를 직접 설계하고, 그 위에서 기능을 개발합니다.

> Spring Boot 3.5 · Java 21 · JPA · MySQL · Redis · ArchUnit · GitHub Actions · Claude Code

---

## 이 저장소에서 볼 것

- **에이전트가 읽는 문서를 어떻게 설계했나** — 정본 규칙 · ID 체계 · 검증 기준 · ADR → [`docs/`](docs/README.md)
- **규칙을 어떻게 강제하나** — ArchUnit 게이트 · 기존 위반 동결(래칫) · 훅 · 권한 → [`docs/04-harness/harness.md`](docs/04-harness/harness.md)
- **작업 하나가 어떻게 흘러가나** — 백로그 티켓 → 이슈 → 계획 → 구현 ⇄ 게이트 → PR → [`docs/04-harness/loop.md`](docs/04-harness/loop.md)
- **무엇이 샜고 어떻게 고쳤나** — 에이전트 사고 기록과 처방 → [`docs/04-harness/incidents.md`](docs/04-harness/incidents.md)
- **지금 어디까지 왔나** — 끝난 것 · 진행 중 · 남은 것 → [`docs/01-product/backlog.md`](docs/01-product/backlog.md)

---

## 무엇을 만드나

판매자가 전자기기 전용 정보(모델 · 배터리 성능 · 구성품 · 하자)로 상품을 올리고, 구매 희망자와 1:1 채팅으로 약속을 잡아 **직접 만나서** 거래합니다.

```
판매중 ──(채팅 상대 한 명을 예약)──▶ 예약중 ──(만나서 거래)──▶ 거래완료
   ◀──────────(예약 해제)─────────┘
```

- **기술 목표** — 한 상품을 두고 여러 사람이 동시에 움직이는 자리(채팅방 열기 · 예약 · 거래완료)의 **동시성 · 정합성**, 채팅 · 알림의 **비동기 메시징**
- **만들지 않는 것** — 결제 · 검수 · 배송 · 재고 수량 · 장바구니 · 경매 ([`PRD.md`](docs/01-product/PRD.md) 「하지 않는 것」)

---

## 어떻게 개발하나

사람은 **방향**을 보고, 기계가 판정할 수 있는 건 **기계**가 합니다.

```
 사람: "T-04 시작"   (백로그의 티켓)
   │
 [0] 백로그 → 이슈 생성 · 브랜치              ★ 사람: 범위 승인
 [1] 요구사항 · 불변식 · ADR → 테스트 목록
     └ 정하지 않은 결정 · 새 갈림길 → 멈추고 묻는다
 [2] 계획을 이슈에 게시                       ★ 사람: 계획 승인
     └ 훅: 계획 게시 전에는 src/ 수정 불가
 ┌ [3] 구현 (계획 한 줄 = 커밋 하나) ─────────────┐
 │ [4] 끝내려 하면 게이트 자동 실행 (ArchUnit)      │  ← 실패하면 이유를 읽고 스스로 고친다
 └──────────── ❌ 이면 [3] 으로 · 3번이면 사람 ──┘
 [5] 전체 빌드 → PR                         ★ 사람: 머지
 [6] 샌 곳이 있으면 사고 기록 → 하네스를 고치는 티켓으로
```

하네스는 세 종류의 장치로 이루어져 있습니다.

- **알려준다** — `CLAUDE.md` · `.claude/rules/` (계층별) · `.claude/skills/ticket` (루프 절차) · `docs/`
- **막는다** — `.claude/settings.json` 권한 · 훅 (`guard-plan` · `guard-protected`)
- **판정한다** — ArchUnit 게이트 (`./gradlew architectureTest`, 약 4초) · 훅 `gate-on-stop` · CI

---

## 문서

| 폴더 | 무엇 |
|---|---|
| [`docs/01-product/`](docs/01-product/) | PRD · 요구사항과 검증 기준 · **백로그** |
| [`docs/02-design/`](docs/02-design/) | 도메인 모델(불변식 · 상태 전이) · API 계약 · [ADR](docs/02-design/adr/README.md) |
| [`docs/03-convention/`](docs/03-convention/) | 아키텍처 · 코드 · 테스트 컨벤션 |
| [`docs/04-harness/`](docs/04-harness/) | 하네스 설계도 · 루프 설계서 · 사고 기록 |

어디에 무엇이 있고 둘이 다르면 무엇을 따르는지는 [`docs/README.md`](docs/README.md) 에 있습니다.

---

## 실행

```bash
# 게이트만 (Docker 불필요)
./gradlew architectureTest

# 전체 테스트 (Docker 필요 — Testcontainers 가 MySQL · Redis 를 띄운다)
AWS_ACCESS_KEY=test AWS_SECRET_KEY=test ./gradlew build

# 로컬 실행 (앱 + Testcontainers MySQL · Redis)
./gradlew bootTestRun
```

---

## 개발 기록

과정은 블로그 시리즈 **[하네스 루프 엔지니어링](https://jdh-devlog.tistory.com)** 에 씁니다 —
개념 · 컨텍스트 설계 · 하네스 구축 · 루프 설계 · 사용 설명서 · 첫 루프(예상 vs 실제).

## 이력

- **2026.06 ~ 08** — 2인 팀 프로젝트로 시작 (검수 에스크로 기반 배송 거래, 공동 작업: [@wlsh44](https://github.com/wlsh44))
- **2026.09 ~** — 혼자 이어서 개발. 제품 방향을 동네 직거래로 바꾸고, 에이전트 컨텍스트 · 하네스 · 루프를 처음부터 다시 설계
- 하네스의 구조(정본 규칙 · ID 체계 · 계획 게시 가드)는 팀 프로젝트 덕모임의 하네스(설계: Seulgi Han)를 참고했습니다
