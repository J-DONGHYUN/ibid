# 코드 컨벤션

## 주석

- [G] **프로덕션 코드에 주석을 달지 않는다.** 의도는 메서드 · 변수 이름으로 드러낸다.
  테스트의 `// given` · `// when` · `// then` 만 예외다 (`SourceRulesTest`, 기존 파일은 동결)
- 왜 그렇게 했는지는 커밋 메시지와 ADR 에 남긴다. 코드 옆에 두면 코드가 바뀔 때 같이 낡는다

## 예외

- 비즈니스 규칙 위반은 `throw new BusinessException(ErrorCode.X)` 로 던진다.
  인증 · 토큰 계열(인터셉터 · 토큰 파싱)은 `GlobalException` 을 쓴다
- **서비스에서 예외를 잡아 다른 응답으로 바꾸지 않는다.** 응답 변환은 `GlobalExceptionHandler` 한 곳이 한다.
  예외는 `ADR-0005` 의 멱등 요청 하나다
- 새 `ErrorCode` 를 만들기 전에 같은 의미가 있는지 본다

## DTO · 커맨드

- 요청 · 응답 DTO 는 `record` 로, `presentation/dto` 에 둔다. 이름은 `~Request` · `~Response`
- 서비스는 **커맨드(`~Command`, `application` 에 둔 `record`)** 를 받는다. 요청 DTO 를 그대로 넘기지 않는다
- 엔티티를 응답으로 그대로 내보내지 않는다

## 저장소 조회

- [G] **상품은 용도에 맞는 조회로 읽는다.** 쓰기 경로(상태를 바꾸거나 상품에 새로 무언가를 붙이는 것)는
  `findActiveById` 로 상품을 얻는다 — 삭제된 상품을 제외한다. 삭제된 상품도 읽어야 하는 조회(기존 채팅방 ·
  채팅방 목록의 상품 요약 · 관심 목록)는 `findIncludingDeleted` · `findAllIncludingDeleted` 를 쓴다.
  `findById` · `existsById` · `findAllById` 는 `application` 에서 부르지 않는다 (`ArchitectureRulesTest`,
  남은 2곳은 동결 → `T-42` · `T-53` 이 지운다 (`T-39` 가 2곳을 지웠다), `ADR-0013`, `I-13`)
- 게이트는 호출을 세는 것이지 올바른 조회를 고르는 것까지는 보장하지 않는다. 쓰기 경로에서 삭제 포함 조회를 쓰면
  게이트는 초록인 채 샌다 — 연산마다 "삭제된 상품이면 거부된다" 테스트를 쓴다

## 엔티티

- 생성은 정적 팩토리 `create(...)` 로 하고, 생성 시 불변식을 검증한다
- `@NoArgsConstructor(access = PROTECTED)` · `@Getter` 를 쓴다
- [G] `@Setter` · `@Data` 를 쓰지 않는다. 상태는 의미 있는 이름의 메서드로만 바꾼다
  (`reserve` · `complete`) (`SourceRulesTest`, 기존 파일은 동결)
- 다른 애그리게이트는 id 로만 참조한다 (`domain-model.md` 애그리게이트)

## 의존성 추가

- **새 라이브러리 · 인프라(RabbitMQ 등)는 ADR 없이 추가하지 않는다.** 필요가 확인되기 전에
  도구를 늘리지 않는다. `build.gradle` 의 의존성을 바꾸는 변경에는 근거 ADR 을 적는다
