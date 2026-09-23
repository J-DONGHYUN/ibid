---
paths:
  - "src/main/java/**/infra/**"
---

# infra 계층

기술 의존을 가둔다. Spring Data 저장소 · Redis · S3 · (도입되면) Kafka.

- 저장소 인터페이스는 여기 두고 Spring Data 를 상속한다 (`ADR-0001`)
- `@Transactional` 을 두지 않는다. 트랜잭션은 `application` 이 연다
- 외부 응답 DTO 를 위 계층으로 그대로 올리지 않는다
- **동시 요청으로 중복될 수 있는 것은 DB 유니크 제약을 건다** (`ADR-0005`). 엔티티의 `@Table(uniqueConstraints)` 로
  선언한다. 스키마가 `ddl-auto: update` 라 제약을 코드에서 선언하지 않으면 DB 에 생기지 않는다
- 새 인프라(Kafka 등)는 ADR 없이 추가하지 않는다
