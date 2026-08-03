# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**일반 중고 거래 + 검수 에스크로** 이커머스 애플리케이션. 모든 거래를 **배송으로 진행**하고 플랫폼이 검수해 안전을 보장합니다(고정가 즉시구매 · 전 거래 강제 검수 · **직거래/채팅 없음**). 배송비는 결제 시 합산되어 플랫폼이 수취·관리합니다. Spring Boot 기반 백엔드입니다.

- **성격**: **포트폴리오 프로젝트** — 동작하는 기능뿐 아니라 **기술적 성장(설계·동시성·이벤트기반·인프라·트러블슈팅)** 을 보여주는 것을 우선합니다. 기술/설계 선택 시 이 관점을 반영하세요.
- **artifact / root project 이름**: `ibid` (`settings.gradle`, `application.yml`의 `spring.application.name`)
- **base 패키지**: `project.kjhjdh.ibid`
- **스택**: Spring Boot 3.5.16, Java 21 (toolchain), Gradle 8.14.5 (wrapper), Spring MVC + Spring Data JPA, Spring Data Redis, Bean Validation, Spring Security Crypto(BCrypt), JJWT, Lombok
- **프론트엔드**: `frontend/` (Next.js) — 백엔드와 별도

## 작업 전 읽을 문서 (중요)

기능 작업 전 관련 문서를 **먼저 읽고** 시작하세요. 현재 구현 상태·플로우·담당 경계가 여기에 정리돼 있습니다.

- **제품 방향·정체성·범위** → `docs/PRODUCT_PLAN.md` (무엇을/누구를 위해 만드는가 — 중고 전자기기 검수 마켓)
- **주문/구매/검수/결제 등 기능 흐름 작업** → `docs/FEATURE_SPEC.md` (기능 정의·전체 플로우·담당 경계)
- **무엇을 할지/진행 상태** → `docs/MVP.md`(MVP1·2 완료 기록) · `docs/BACKLOG.md`(MVP3+ 전체 백로그·2인 분담)
- **결제 프로세스 현재 상태·알려진 결함** → `docs/PAYMENT_PROCESS.md`
- **테스트 코드 작성** → `docs/TEST.md` (계층/네이밍/Fixture/DbCleaner 규칙)

기능을 추가·변경하면 `docs/FEATURE_SPEC.md`와 `docs/MVP.md`를 최신 상태로 갱신하세요.

## 도메인 구조

도메인별 패키지 아래에 `presentation` / `application` / `domain` / `infra` 4계층으로 배치합니다. 새 도메인도 이 구조를 따르세요.

```
project.kjhjdh.ibid
├── auth       인증 (JWT 발급/검증, 리프레시 토큰 회전 — Redis 저장, 쿠키 전달)
├── user       사용자 (email 값객체, BCrypt 비밀번호)
├── product    상품 (등록/수정/삭제·목록[커서 페이지네이션]/상세, 상태 PENDING→ON_SALE→SOLD_OUT, 재고 차감/복원 — 이미지 ProductImage를 자기 도메인이 소유, S3 기술은 common.image 재사용)
├── order      주문 (7상태 상태기계, 재고 비관적 락 동시성 제어, 본인 거래 차단 — payment를 모름)
├── inspection 검수 (수령/합격/불합격 판정 기록 + 판정 도메인 이벤트 발행)
├── payment    결제 (Toss 연동, 결제 승인/취소, order 호출·검수 이벤트 구독 — 팀원 담당)
├── common     공통 (예외 체계 ErrorCode/BusinessException/GlobalExceptionHandler, CORS/Web 설정)
└── common.image  이미지 S3 기술만 (presigned 업로드/삭제 S3ImageUploader·확장자 enum) — **도메인 비의존**. 이미지 소유·연관은 각 도메인이 가짐(예: product.ProductImage). 의존 방향: 도메인 → common.image (역방향 금지)
```

- 예외는 `ErrorCode` enum + `BusinessException`으로 던지고, 응답 변환은 `GlobalExceptionHandler`가 담당합니다.
- 인증이 필요한 컨트롤러는 `@LoginUser UserInfo`로 로그인 사용자를 주입받습니다(`AuthenticationInterceptor` + `LoginUserArgumentResolver`).
- **의존성 방향**: `payment → order`. order/inspection은 payment를 import하지 않습니다. 정산/환불은 inspection이 **도메인 이벤트**(`InspectionPassed`/`InspectionFailed`) 발행 → payment가 구독.
- **향후 도메인**(notification, chat, review, community, search, file 등)도 같은 4계층 + 이벤트 원칙을 따르세요. 전체 계획은 `docs/BACKLOG.md`.

## 명령어

Gradle wrapper(`./gradlew`)를 사용합니다. 별도 설치 불필요.

```bash
./gradlew build              # 컴파일 + 테스트 + jar 빌드
./gradlew test               # 전체 테스트 (Docker 필요, 아래 참고)
./gradlew bootRun            # 앱 실행 (운영/일반 DB 설정 사용)
./gradlew classes            # 프로덕션 소스만 컴파일 (빠른 확인용)
```

단일 테스트 실행:

```bash
./gradlew test --tests 'project.kjhjdh.ibid.IbidApplicationTests'
./gradlew test --tests '*.SomeClass.someMethod'   # 메서드 단위
```

로컬 개발 실행 (Testcontainers로 MySQL을 자동 기동):

```bash
./gradlew bootTestRun    # TestIbidApplication을 실행 — 앱 + Testcontainers MySQL
```

## DB / 테스트 아키텍처

DB 구성이 코드에 아직 명시돼 있지 않다는 점이 이 저장소의 핵심 함정입니다.

- 설정은 `application.yml`(공통, 기본 활성 프로파일 `local`) + `application-local.yml`(로컬 전용 — **H2 인메모리** datasource, `MODE=MySQL`)로 구성됩니다. 따라서 `bootRun`(`IbidApplication`)은 기본적으로 **H2 인메모리**로 뜹니다(앱 재시작 시 데이터 소멸, Redis는 `localhost:6379`가 별도로 필요).
- **런타임 의존성**으로 H2(embedded)와 MySQL connector가 **둘 다** 들어 있습니다.
- **테스트와 로컬 개발 실행은 Testcontainers(MySQL `mysql:8.4` + Redis `redis:latest`)에 의존**합니다 (`TestcontainersConfiguration`). 따라서 `./gradlew test`, `./gradlew bootTestRun`을 돌리려면 **Docker 데몬이 실행 중이어야** 합니다. Docker가 없으면 테스트가 실패합니다.
- 두 컨테이너 모두 `@ServiceConnection`으로 Spring Boot에 자동 연결됩니다 — 테스트에서 datasource/redis 접속 정보를 직접 지정할 필요 없음. 인증(리프레시 토큰)이 Redis를 사용합니다.
- `TestIbidApplication`은 프로덕션 `IbidApplication`을 Testcontainers 설정과 함께 띄우는 개발 전용 진입점입니다 (`SpringApplication.from(...).with(...)`).

## 금지사항

- **기능 구현 시 주석을 달지 마세요.** 코드로 의도가 드러나도록 작성하고, 설명이 필요하면 주석 대신 메서드/변수 이름으로 표현하세요. (테스트의 given/when/then 구역 주석은 예외)

## 협업 컨벤션 (`.github/`)

- 팀은 한국어로 협업하며 이슈/PR 템플릿이 정의돼 있습니다.
- PR 체크리스트가 **커밋 메시지 컨벤션 준수**와 **테스트 코드 작성**을 요구합니다 — 변경 시 함께 지키세요.
- 작업은 이슈(`enhancement` 라벨) → PR(`Closes #이슈번호`) 흐름을 따릅니다.

## 참고 문서

- `docs/PRODUCT_PLAN.md` : 제품 방향·정체성·범위 (중고 전자기기 검수 마켓)
- `docs/BACKLOG.md` : MVP3+ 전체 개발 백로그 · 2인 분담 · 포트폴리오 기술 매핑
- `docs/FEATURE_SPEC.md` : 기능 정의·전체 플로우·담당 경계 (기능 작업 전 필독)
- `docs/MVP.md` : MVP1·2 작업 기록
- `docs/PAYMENT_PROCESS.md` : 결제 프로세스 현재 상태·알려진 결함
- `docs/API.md` : REST API 명세(엔드포인트 종합)
- `docs/TEST.md` : 테스트 작성 규칙
