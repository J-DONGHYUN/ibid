# 하네스 설계도

ibid 의 하네스가 **무엇으로 이루어져 있고, 왜 그렇게 만들었는지**를 적는다.
주 독자는 하네스를 고치려는 사람이다. 에이전트는 매 세션 읽지 않는다.

## 정의

하네스는 에이전트에게 잘 부탁하는 법이 아니라, **사람이 매번 다시 설명하지 않아도 되게 만드는 저장소 설정**이다.
부탁은 지켜지지 않을 수 있고, 설정은 지켜진다.

하네스 안의 장치는 하는 일에 따라 셋이다.

| 종류 | 하는 일 | 모델이 무시할 수 있나 |
|---|---|---|
| ① 알려주기 | 텍스트를 모델의 컨텍스트에 넣는다 | 있다 |
| ② 막기 | 행동을 실행 전에 차단하거나 사람에게 묻게 한다 | 없다 |
| ③ 판정 | 결과를 기계가 채점한다. 실패 메시지는 다시 ①이 된다 | 없다 |
| ④ 검토 | 만들지 않은 모델이 결과를 읽고 의견을 낸다 | 있다 — 그래서 조치를 PR 에 적게 한다 |

## 지금 구성

| 종류 | 장치 | 언제 | 무엇을 |
|---|---|---|---|
| ① | `CLAUDE.md` + `@docs/README.md` | 매 세션 | 명령, 이행기 코드 상태, 겪은 함정, 문서 지도 |
| ① | `.claude/rules/*.md` | 해당 경로를 수정할 때 | 계층별 규칙과 자주 틀리는 것 |
| ① | `docs/` | 필요할 때 | 요구사항 · 불변식 · ADR · 컨벤션 (정본) |
| ① | `.claude/skills/ticket/` | 이슈 작업을 시작할 때 | 루프 순서와 명령 (`loop.md`) |
| ② | `.claude/settings.json` `ask` | 도구 호출 전 | 게이트 · 동결 기록 · 빌드 · CI · 권한 · 훅 · 리뷰어 파일 수정, 계획 게시 · 이슈 생성 · PR 생성 · `git push` |
| ② | `.claude/settings.json` `deny` | 도구 호출 전 | 강제 push |
| ② | `hooks/guard-protected.sh` | Bash 실행 전 | 셸 명령으로 게이트 · 동결 기록 · 빌드 설정 · 훅 · 리뷰어를 고치면 `ask` |
| ② | `hooks/reviewer-readonly.sh` | 리뷰어의 Bash 실행 전 | 읽기 명령(`git diff` · `gh issue view` …)만 통과, 나머지 `deny`. 리뷰어 frontmatter 에만 걸린다 |
| ② | `hooks/guard-plan.sh` | 파일 수정 · Bash 실행 전 | 이슈 브랜치에서 계획을 게시하기 전에는 `src/` 수정 금지 |
| ③ | `hooks/gate-on-stop.sh` | 에이전트가 끝내려 할 때 | `src/` 가 바뀌었으면 `architectureTest`. 실패하면 못 끝냄, 3회 연속이면 멈추고 사람을 부름 |
| ③ | `./gradlew architectureTest` | 에이전트가 자주 | 아키텍처 · 소스 · 테스트 컨벤션 · 공개 경로. **약 4초**, 스프링 · Docker 없음 |
| ③ | `./gradlew test` | 구현을 마칠 때 | 전체 테스트. **약 1분 20초**, Docker 필요 |
| ③ | `.github/workflows/ci.yml` | PR · push | 위 둘을 순서대로. 로컬과 **같은 게이트** |
| ④ | `.claude/agents/reviewer.md` | PR 전, 전체 빌드 뒤 | 이슈 번호 · 브랜치만 받아 리뷰. 🔴 · 🟡 · ⚪ 로 돌려준다 (`loop.md`) |

## 게이트

`[G]` 로 표시한 규칙을 옮긴 것이다. 실패 메시지(`because` · `as`)에 **근거 문서의 위치**를 적었다 —
게이트가 막으면 그 메시지가 에이전트의 다음 컨텍스트가 되기 때문이다. "무엇이 틀렸다"만이 아니라
"어디를 읽으면 되는지"까지 알려줘야 우회하지 않고 고친다.

| 게이트 | 규칙 | 근거 | 동결 |
|---|---|---|---|
| `ArchitectureRulesTest` | 도메인 의존 방향 · 삭제 예정 도메인 의존 금지 | `architecture.md` · `ADR-0006` | 동결 |
| 〃 | `application` → `presentation` 금지 | `code.md` 「DTO · 커맨드」 | 동결 |
| 〃 | `domain` 은 스프링 · 바깥 계층에 의존하지 않는다 | `architecture.md` 「계층」 | — |
| 〃 | `@Transactional` 은 `application` 에만 | 〃 | 메서드 규칙만 동결 |
| 〃 | 저장소 인터페이스는 `infra` 에 | `ADR-0001` | — |
| `TestConventionRulesTest` | `@Test` 에 `@DisplayName` | `test.md` | 동결 |
| 〃 | 테스트에 `@Transactional` 금지 · AssertJ 사용 | 〃 | — |
| `SourceRulesTest` | 주석 금지 · `@Setter` · `@Data` 금지 | `code.md` | 목록으로 동결 |
| `PublicEndpointRulesTest` | 경로 공개 목록은 `/api/auth/**` 하나 | `ADR-0003` | — |

## 래칫 — 기존 위반을 동결한다

ibid 는 코드가 먼저 있고 하네스가 나중에 왔다. 게이트를 켜는 순간 기존 코드가 이미 규칙을 어기고 있다.
그래서 **기존 위반은 동결하고 새 위반만 막는다** (`ADR-0010`).

- **동결 기록이 남은 기술 부채의 정본이다.** 문서에 "현재 위반 몇 개"를 적지 않는다 — 적는 순간 낡는다 (`INC-01`)
  - ArchUnit: `src/test/resources/archunit-store/` (파일 하나가 규칙 하나, 줄 하나가 위반 하나)
  - 소스 검사: `SourceRulesTest` 의 `FROZEN_*` 목록
- **고치면 줄어든다.** ArchUnit 은 고친 위반을 기록에서 스스로 지운다. 소스 검사는 고쳤는데 목록에 남아 있으면
  실패해서 목록을 줄이게 한다
- **늘리면 게이트 우회다.** 동결 기록 · 게이트 파일 수정은 `ask` 로 사람을 거친다
- **저장소 생성을 막아 둔다** (`allowStoreCreation=false`). 저장소가 통째로 사라지면 게이트가 전부 조용히 다시
  동결하는 대신 실패한다
- **새 규칙을 동결 규칙으로 추가하면 그 순간의 위반이 기록된다.** 그래서 새 규칙은 사람이 추가하고, 기록된
  위반 수를 PR 에 적는다

```bash
wc -l src/test/resources/archunit-store/*-*    # 규칙별 남은 위반 수
```

## 생존 확인 — 초록불이 무엇을 증명하나

게이트가 통과한다고 규칙이 살아 있다는 뜻은 아니다. 동결이 새 위반까지 삼키거나, 대상 클래스가 없어 빈 채로
통과할 수 있다. 그래서 **게이트를 만들거나 바꾸면 일부러 어긴 코드(탐침)로 막히는지 확인한다.**

2026-09-28 첫 확인:

| 탐침 | 결과 |
|---|---|
| `product.application` 에 `order` 를 import 하고 요청 DTO 를 받는 클래스, 주석 한 줄 | 도메인 방향 · `application` → `presentation` · 주석 3개 게이트 모두 실패. 동결된 기존 71건이 아니라 **새 1건만** 보고 |
| `WebConfig.PUBLIC_ENDPOINTS` 에 `/api/products/**` 추가 | 공개 경로 게이트 실패 |
| 탐침 제거 후 | 통과. 동결 기록에 탐침이 들어가지 않음 |

## 사람의 자리

게이트가 판정할 수 있는 것에는 사람을 넣지 않는다. 사람은 게이트가 판정할 수 없는 것만 본다.

- **게이트 자체를 바꾸는 것** — 규칙 · 동결 기록 · 빌드 설정 (`ask`)
- **밖으로 나가는 것** — `git push` (`ask`)
- **방향** — 계획 · `제안` 상태 ADR 의 결정 (루프에서 정한다)

## 덕모임 하네스와 다른 점

구조(정본 규칙 · ID 체계 · 게이트 · 사람의 자리)는 덕모임 팀 하네스(설계: Seulgi Han)를 참고했다. 다른 점은 이렇다.

| | 덕모임 | ibid | 이유 |
|---|---|---|---|
| 시작 | 하네스와 코드가 함께 시작 | 코드가 먼저 있음 | 그래서 **래칫**이 필요하다 |
| 막을 상대 | 백엔드 3명의 에이전트 | 혼자 | CODEOWNERS · 브랜치 보호 · Jira 연동을 두지 않는다 |
| 작업 단위 | Jira 티켓 | GitHub Issue | 혼자라 연동 자동화가 필요 없다 |
| 문서 위치 | 위키 저장소(서브모듈) | 같은 저장소 `docs/` | 서브모듈 동기화 문제가 없다 |

## 알려진 구멍

- **Bash 우회는 일부만 막는다.** `guard-protected.sh` · `guard-plan.sh` 는 `write-target.sh` 로 명령의 **쓰기 대상**을
  뽑아(리다이렉션의 파일 · cp/mv 도착지 · `sed -i`·`rm`·`tee`·`git checkout` 등의 경로 인자) 그 대상이 보호 경로 · `src/`
  일 때만 막는다 (`INC-03`, T-30). 하지만 `python -c` 처럼 인터프리터 안에서 쓰는 경우는 명령 문자열만 봐서는 알 수 없다
- **`write-target.sh` 는 리다이렉션을 따옴표 없이 grep 한다.** 따옴표로 감싼 문자열 속 `>`(예: `echo 'a > b'`)도
  리다이렉션으로 봐서 대상을 뽑는다. `sed -i` 는 스크립트 인자까지 대상으로 본다 — 넘겨짚어 `ask` 하는 쪽이라 막는 경로는 줄지 않는다
- **훅은 이 저장소에서 Claude Code 를 열 때만 걸린다.** 상위 폴더에서 세션을 열면 `.claude/settings.json` 이 적용되지 않는다
- **아직 없는 도메인(`chat` · `trade` · `notification`)의 규칙은 빈 채로 통과한다.** 도메인을 처음 만드는 티켓에서
  탐침으로 생존을 확인한다
- **주석 검사는 줄 시작만 본다.** 코드 뒤에 붙은 주석(`x = 1; // …`)은 못 잡는다
- **`@DisplayName` 의 요구사항 ID 규칙(`[TR-01] …`)은 아직 게이트가 없다.** 기존 테스트 전부가 어기고 있어,
  요구사항 커버리지 측정과 함께 만든다
- **에이전트 정의는 세션을 시작할 때 읽힌다.** 리뷰어를 만들거나 고친 세션에서는 새 리뷰어를 부를 수 없다.
  그런 티켓은 새 세션에서 [5] 를 이어 한다 (`INC-06`)
- **리뷰어의 Bash 화이트리스트는 따옴표를 모른다.** `;` · `|` · `&` 로 조각을 나눠서 `grep -E 'a|b'` 는 막힌다.
  오탐이지만 새지는 않는다 — 리뷰어는 Grep 도구를 쓰면 된다
- 루프 자체의 한계는 `loop.md` 「알려진 한계」

## 훅 생존 확인

2026-09-28, 훅에 만든 입력을 넣어 확인했다.

| 탐침 | 결과 |
|---|---|
| `sed -i` 로 동결 기록 · 리다이렉션으로 `build.gradle` · `git checkout` 으로 게이트 테스트 | `guard-protected` 가 `ask` |
| `wc` 로 동결 기록 읽기 · 보호 안 된 파일 `sed -i` | 통과 |
| `develop` 에서 `src/` 수정 · Bash 로 `src/` 에 쓰기 | `guard-plan` 이 `deny` (이슈 브랜치가 아님) |
| 이슈 브랜치인데 없는 이슈(#999) · 계획 없는 이슈(#36) | `deny`. 막힌 결과는 캐시하지 않음 |
| `develop` 에서 `docs/` 수정 · Bash 로 `src/` 읽기 | 통과 |
| src/ 변경 없이 끝내기 | 게이트 생략 |
| `order` 를 import 하는 탐침을 두고 끝내기 3번 | 1 · 2번째 `block` (이유 · 위반 위치 포함), 3번째 멈춤 알림. 로그에 `FAIL 1~3` · `STOP_FOR_HUMAN` |

**첫 루프(T-04 · #71 · PR #72, 2026-09-28)에서 실제로 확인한 것**

- `guard-plan` — 계획 게시 후 `src/` 삭제가 통과했다 (`plan-posted-71` 캐시)
- `guard-protected` — 막아야 할 명령은 없었고, **오탐 2번** (`INC-03`)
- `gate-on-stop` — **게이트를 한 번도 돌리지 않았다.** 커밋 뒤 작업 트리가 깨끗해서 "변경 없음"으로 봤다 (`INC-02`)
- 래칫 — 지운 코드의 위반이 동결 기록에서 저절로 빠졌다 (`c4008cc5` 71→41줄)

**T-29 (#75, 2026-09-28) — `gate-on-stop` 을 고친 뒤 다시 확인**

`INC-02` 를 고쳐, 감지 기준을 "`main` 과 비교한 브랜치 커밋 + 커밋 안 된 변경"으로 바꾸고
`src/`·`build.gradle` 트리 해시로 같은 상태 재실행을 생략하게 했다. 탐침 src 커밋을 브랜치에 얹고
훅을 직접 실행해 확인한 뒤 되돌렸다.

| 탐침 | 결과 |
|---|---|
| 유효한 src 를 커밋해 작업 트리가 깨끗한 채로 끝내기 | 게이트가 돈다. 로그 `PASS <해시>` — 고치기 전엔 여기서 생략했다 (`INC-02`) |
| 같은 상태로 다시 끝내기 | 게이트 생략. 로그 `SKIP` (126ms) |
| 문서만 고치고 끝내기 | 게이트 생략. 코드 지문이 그대로다 |
| 주석 한 줄(위반)을 커밋하고 끝내기 | `block`. 이유·위반 파일(`probe/Probe.java`) 포함, 로그 `FAIL 1~2` |

**T-31 (#77, 2026-09-28) — 리뷰어 읽기 전용 훅**

`reviewer-readonly.sh` 에 만든 입력을 넣어 확인했다.

| 탐침 | 결과 |
|---|---|
| `git diff main...<브랜치>` · `git log` · `gh issue view` · `gh pr view` · `git -C … --no-pager log` · `cd … && git status` | 통과 |
| `… \| sort` · `… \| uniq -c` (파이프 뒤 정렬 · 중복제거, 계획대로) | 통과 |
| `git diff … \| grep` · `git show … 2>/dev/null \| head` | 통과 (`2>/dev/null` 은 쓰기로 보지 않는다, `INC-03`) |
| 리다이렉션(`>` · `>>` · heredoc) · `$( )` · 백틱 | `deny` |
| `git commit` · `gh issue edit` · `gh api -X DELETE` · `sed -i` · `python3 -c` | `deny` |
| `git diff --output=…` · `git grep -O` · `git -c core.pager=…` | `deny` (git 이 파일을 쓰거나 프로그램을 부른다) |
| `git diff; rm x` · `git diff && git checkout .` | `deny` (조각마다 본다) |
| `git log --grep="a\|b"` | `deny` — 오탐 (알려진 구멍) |

`guard-protected.sh` 는 `sed -i` · `rm` 으로 `.claude/agents/` 를 고치면 `ask`, `cat` 은 통과했다.

2026-09-29, 실제 경로로도 확인했다 — 새 세션에서 `reviewer` 를 불러 이 브랜치를 리뷰시켰고(INC-06 처방),
그 리뷰어의 Bash 탐침에 쓴 `$( )` 명령 치환이 훅에 막혀 스스로 훅을 재실행하지 못했다. 만든 입력이 아니라
실제 서브에이전트 경로에서 명령 치환 차단이 살아 있다는 방증이다.

**T-30 (#80, 2026-09-29) — 쓰기 대상 판정(`write-target.sh`) 로 오탐 제거**

`guard-protected` · `guard-plan` 을 `write-target.sh` 기반으로 바꾼 뒤, 탐침을 파일에 담아
훅에 넣어 확인했다(명령 문자열에 보호 리터럴을 두면 훅 자신이 걸려서다). 실제로 이 티켓의 이슈 생성도
옛 오탐(`gh issue create` 본문에 `src/` · `>`)에 한 번 막혀, 초안을 Write 도구로 만들어 우회했다.

| 탐침 | 결과 |
|---|---|
| `… 2>&1 \| tail \| wc -l …/archunit-store/…` · `cat > pr-71.md <<'EOF' … archunit-store …` (INC-03 두 명령) | 통과 (오탐 사라짐) |
| `./gradlew test 2>&1 \| grep src/` (guard-plan) | 통과 (`src/` 는 grep 인자, 쓰기 아님) |
| `cp build.gradle /tmp/x` (원본만 보호) · `sed`(−i 없음) · `git checkout main`(브랜치 전환) | 통과 |
| `cat > build.gradle` · `2> build.gradle` · `>> .github/workflows/ci.yml` · `… \| tee build.gradle` | `ask` |
| `cp foo build.gradle` · `mv foo .claude/settings.json`(도착지) · `ls && sed -i … build.gradle`(둘째 조각) · `> "build.gradle"`(따옴표) | `ask` |
| `sed -i …/archunit-store/…` · `git checkout .claude/settings.json` (기존 막는 경로) | `ask` |

승인 메시지에 **걸린 파일 · 걸린 명령 · 승인/거절 기준**을 사람이 읽는 문장으로 담는다.

## 다음

1. **첫 루프에서 샌 곳을 고친다** — 백로그 「하네스 개선」 T-29 ~ T-32 (`INC-02` ~ `INC-05`)
2. 루프 기록이 쌓이면 티켓당 게이트 실패 · 사람 개입을 보고 하네스를 고친다
