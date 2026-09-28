# ibid 작업 규칙

동네에서 만나 거래하는 중고 전자기기 마켓의 API 서버. Spring Boot 3.5 · Java 21 · Gradle.

무엇을 · 왜는 `docs/` 가 정본이다. 이 문서는 **`docs/` 에 없는 것만** 담는다 —
명령, 지금 코드의 상태, 겪은 함정.

@docs/README.md

## 작업 전에 읽는다

| 작업 | 읽을 것 |
|---|---|
| 기능 구현 · 수정 | `requirements.md` 의 해당 ID → `domain-model.md` 의 관련 불변식 → 걸린 ADR |
| 상품 상태 · 예약 · 거래완료 | `domain-model.md` 상품 상태 전이 · `ADR-0006` · `ADR-0008` |
| 새 패키지 · 클래스 위치 판단 | `03-convention/architecture.md` |
| 테스트 작성 | `03-convention/test.md` |
| API 추가 | `02-design/api.md` |

**`제안` 상태 ADR 에 걸린 요구사항은 구현을 시작하지 않는다.** 선택지를 정리해 사람에게 묻는다.
지금은 `ADR-0007`(채팅 전달) · `ADR-0008`(전이 동시성) · `ADR-0009`(Kafka) · `ADR-0011`(상품 삭제 방식)이 `제안` 이다.

**ADR 에 없는 갈림길을 만나면 고르지 않는다.** 기존 ADR 과 컨벤션으로 답이 안 나오는 설계 결정 —
대안이 여럿이고, 되돌리기 비싸고, 흔한 방법이 이 프로젝트에서 틀릴 수 있는 것 — 은 `제안` ADR 초안
(맥락 · 선택지 · 결정 기준)을 만들어 사람에게 묻는다. **결정은 사람이 한다.** 에이전트는 조사와 초안을 맡는다.
변수 이름 · 메서드 분리처럼 쉽게 되돌릴 수 있는 것은 ADR 없이 컨벤션대로 한다.

**문서와 코드가 다르면 추측하지 않는다.** 규칙 · 불변식이면 코드가 버그이거나 문서가 낡은 것이다.
어느 쪽인지 사람에게 묻는다.

## 명령

```bash
./gradlew architectureTest  # 게이트만. 약 4초, Docker 불필요. 코드를 바꿀 때마다 돌린다
./gradlew build             # 컴파일 + 테스트 + jar. Docker 필요
./gradlew test --tests '*.ProductServiceTest'
./gradlew compileJava compileTestJava   # 테스트 없이 컴파일만. Docker 불필요
./gradlew bootTestRun       # 앱 + Testcontainers MySQL · Redis (로컬 개발용)
```

## 지금 코드의 상태 — 이행기

`PRD.md` 에서 제품 방향을 바꿨고, 코드에는 이전 제품이 남아 있다.

| 코드 | 상태 | 할 것 |
|---|---|---|
| `order` · `payment` · `inspection` | 삭제 예정 | **확장하지 않는다. 새 코드가 import 하지 않는다** |
| `product` 의 재고(`stock`) · 배송비(`shippingFee`) · `PENDING` · 비관적 락 | 삭제 예정 | 새 코드에서 쓰지 않는다 |
| `product` 의 조회수(`viewCount` · Redis) · 태그(`Tag`) | 요구사항 없음, 결정 대기 | 확장하지 않는다 |
| `frontend/` | 이전 흐름 화면 | 백엔드 작업 중에는 건드리지 않는다 |

## 겪은 함정

- **테스트에 `AWS_ACCESS_KEY` · `AWS_SECRET_KEY` 환경변수가 필요하다.** `application.yml` 이 기본값 없이
  참조하고, 테스트용 설정 파일이 따로 없다. 없으면 스프링 컨텍스트가 뜨지 않는다.
  `AWS_ACCESS_KEY=test AWS_SECRET_KEY=test ./gradlew test`
- **테스트는 Docker 가 켜져 있어야 한다.** Testcontainers 가 MySQL · Redis 를 띄운다. 꺼져 있으면
  통합 테스트가 코드와 무관하게 전부 실패한다 — 실패 원인이 "Docker environment not found" 인지 먼저 본다
- **`bootRun` 은 H2 로 뜬다.** 기본 프로파일 `local` 이 H2 인메모리(`MODE=MySQL`)를 쓴다. 락 · 문법이
  MySQL 과 달라서 동시성 동작은 `bootRun` 으로 확인하지 않는다. `bootTestRun` 을 쓴다
- **스키마는 `ddl-auto: update` 다. 마이그레이션 도구가 없다.** `update` 는 컬럼을 지우지 않는다 —
  필드를 지워도 기존 DB 에는 `NOT NULL` 컬럼이 남아 INSERT 가 실패할 수 있다. 테스트는 매번 새 컨테이너라 드러나지 않는다
- **`ControllerTestSupport` 가 모든 서비스를 `@MockitoBean` 으로 들고 있다.** 서비스를 지우거나 새로
  만들면 여기도 고쳐야 컨트롤러 테스트가 뜬다
- **`TestcontainersConfiguration` 에 시딩용 설정(`SEED_REWRITE_BATCHED_STATEMENTS`)이 남아 있다.**
  시딩 도구는 지웠다. 쓰지 않는다
- **게이트 초록불이 규칙이 살아 있다는 뜻은 아니다.** 아직 없는 도메인(`chat` · `trade` · `notification`)의 규칙은
  대상이 없어 빈 채로 통과한다. 그 도메인을 처음 만들면 일부러 어긴 코드로 게이트가 막는지 확인하고 지운다
  (`04-harness/harness.md` 「생존 확인」)

## 하면 안 되는 것

- 프로덕션 코드에 주석을 달지 않는다 (`03-convention/code.md`)
- 게이트(테스트 · 아키텍처 검사)가 막으면 **코드를 고친다. 규칙이나 테스트를 고쳐 통과시키지 않는다.**
  규칙이 틀렸다고 판단되면 근거를 적어 사람에게 묻는다
- **동결 기록에 위반을 더하지 않는다.** `src/test/resources/archunit-store/` 와 `SourceRulesTest` 의 `FROZEN_*` 목록은
  남은 기술 부채다. 줄이는 것만 한다. 동결된 코드를 고치다 위반이 없어지면 목록에서도 지운다 (`ADR-0010`)
- `PRD.md` 의 `NG` 에 있는 것을 만들지 않는다. 필요해 보이면 묻는다
- ADR 없이 새 라이브러리 · 인프라를 추가하지 않는다

## 기능을 바꾸면 함께 고친다

- `requirements.md` 의 `상태` 열
- 불변식 · 전이가 바뀌면 `domain-model.md`
- 결정이 바뀌면 새 ADR (기존 ADR 은 `대체됨` 으로 두고 지우지 않는다)

## 커밋

- 제목은 `type: 한국어 요약` (`feat` · `fix` · `refactor` · `test` · `docs` · `chore`)
- 본문은 **왜 그렇게 했는지**를 쓴다. 무엇을 했는지는 diff 에 있다
- 요구사항 · 이슈가 있으면 본문에 ID 를 적는다 (`TR-01`, `#12`)
