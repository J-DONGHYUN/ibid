# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

중고 거래 + 검수 에스크로 이커머스 애플리케이션. Spring Boot 기반 백엔드입니다.

- **artifact / root project 이름**: `ibid` (`settings.gradle`, `application.properties`의 `spring.application.name`)
- **base 패키지**: `project.kjhjdh.ibid`
- **스택**: Spring Boot 3.5.16, Java 21 (toolchain), Gradle 8.14.5 (wrapper), Spring MVC + Spring Data JPA, Spring Data Redis, Bean Validation, Spring Security Crypto(BCrypt), JJWT, Lombok
- **프론트엔드**: `frontend/` (Next.js) — 백엔드와 별도

## 작업 전 읽을 문서 (중요)

기능 작업 전 관련 문서를 **먼저 읽고** 시작하세요. 현재 구현 상태·플로우·담당 경계가 여기에 정리돼 있습니다.

- **주문/구매/검수/결제 등 기능 흐름 작업** → `docs/FEATURE_SPEC.md` (기능 정의·전체 플로우·담당 경계)
- **무엇을 할지/진행 상태 확인** → `docs/MVP.md` (작업 목록 T0~, 진행 순서)
- **테스트 코드 작성** → `docs/TEST.md` (계층/네이밍/Fixture/DbCleaner 규칙)

기능을 추가·변경하면 `docs/FEATURE_SPEC.md`와 `docs/MVP.md`를 최신 상태로 갱신하세요.

## 도메인 구조

도메인별 패키지 아래에 `presentation` / `application` / `domain` / `infra` 4계층으로 배치합니다. 새 도메인도 이 구조를 따르세요.

```
project.kjhjdh.ibid
├── auth      인증 (JWT 발급/검증, 리프레시 토큰 회전 — Redis 저장, 쿠키 전달)
├── user      사용자 (email 값객체, BCrypt 비밀번호)
├── product   상품 (등록/목록[커서 페이지네이션]/상세, 상태 PENDING→ON_SALE→SOLD_OUT)
├── order     주문/구매 (재고 차감, 비관적 락 동시성 제어, 본인 거래 차단)
└── common    공통 (예외 체계 ErrorCode/BusinessException/GlobalExceptionHandler, CORS/Web 설정)
```

- 예외는 `ErrorCode` enum + `BusinessException`으로 던지고, 응답 변환은 `GlobalExceptionHandler`가 담당합니다.
- 인증이 필요한 컨트롤러는 `@LoginUser UserInfo`로 로그인 사용자를 주입받습니다(`AuthenticationInterceptor` + `LoginUserArgumentResolver`).

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

- **런타임 의존성**으로 H2(embedded)와 MySQL connector가 **둘 다** 들어 있습니다. `application.properties`에는 datasource 설정이 없으므로, `bootRun`으로 어떤 DB를 쓸지는 접속 정보를 추가해야 결정됩니다.
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

- `docs/FEATURE_SPEC.md` : 기능 정의·전체 플로우·담당 경계 (기능 작업 전 필독)
- `docs/MVP.md` : MVP 작업 목록·진행 상태
- `docs/API.md` : REST API 명세(엔드포인트 종합)
- `docs/TEST.md` : 테스트 작성 규칙
