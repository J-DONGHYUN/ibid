---
paths:
  - "src/main/java/**/application/**"
---

# application 계층

유스케이스의 **흐름**을 표현한다. 메서드를 읽으면 업무 순서가 보여야 한다.

- 트랜잭션 경계는 여기 public 메서드에 둔다. 조회 전용은 `@Transactional(readOnly = true)`
- **`presentation` 을 import 하지 않는다.** 요청 DTO 대신 커맨드(`~Command` record)를 받는다.
  기존 서비스 중 요청 DTO 를 받는 곳이 있다 — 고칠 때 커맨드로 바꾸고, 새 코드에서 따라 하지 않는다
- 예외를 잡아 다른 응답으로 바꾸지 않는다. `throw` 만 한다. 유일한 예외는 멱등 요청(`ADR-0005`)
- 다른 도메인의 서비스를 부르기 전에 `docs/03-convention/architecture.md` 「도메인과 의존 방향」 을 본다.
  **`product` 가 `chat` 을 부르면 순환이다** — 둘을 모두 봐야 하면 `trade` 에 둔다 (`ADR-0006`)
- 삭제 예정 도메인(`order` · `payment` · `inspection`)을 부르지 않는다

동시성이 걸린 유스케이스(채팅방 열기 · 메시지 전송 · 예약 · 거래완료)는
**단일 스레드 테스트로는 검증되지 않는다.** 동시성 테스트를 같이 쓴다 (`docs/03-convention/test.md`).
