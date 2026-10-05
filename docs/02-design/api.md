# API

공통 계약과 인증 등급을 정한다. **요청 · 응답 필드와 에러 코드 목록은 코드가 정본이다**
(`presentation/dto` · `common/exception/ErrorCode`). 여기에 옮겨 적지 않는다.

## 공통 계약

- 모든 경로는 `/api/{복수형 리소스}` 로 시작한다
- JSON 필드명은 camelCase. 이름은 `domain-model.md` 용어 표의 코드 식별자를 따른다
- **상태 전이는 하위 경로에 `POST` 로 둔다.** 필드를 `PATCH` 로 바꾸는 방식으로 상태를 바꾸지 않는다 —
  전이마다 권한 · 선행 상태 검사가 달라서 하나의 수정 API 로 묶으면 검사가 빠진다
  (`POST /api/products/{id}/reservation`)
- 목록은 커서 기반이다. 파라미터는 `cursor` (없으면 처음부터). 응답에 다음 커서를 싣는다 (`ADR-0004`)

### 에러 응답

모든 실패는 `ErrorResponse` 형식 하나로 응답한다. 예외를 던지면 `GlobalExceptionHandler` 가 바꾼다.

```json
{ "code": "PRODUCT_NOT_FOUND", "message": "..." }
```

| 상황 | status |
|---|---|
| 입력 형식 오류 (Bean Validation) | 400 |
| 인증 없음 · 토큰 만료 · 위조 | 401 |
| 로그인했지만 권한 없음 (남의 상품 수정 등) | 403 |
| 대상 없음 | 404 |
| **동시 요청으로 유니크 제약 위반** · 상태가 이미 바뀜 | 409 |

- 새 에러 코드를 만들기 전에 `ErrorCode` 에 같은 의미가 있는지 본다. 같은 의미의 코드를 두 개 만들지 않는다

## 인증 등급

| 등급 | 표시 | 뜻 |
|---|---|---|
| 공개 | `@PublicApi` | 토큰 없이 통과. **토큰을 보냈다면 유효해야 한다** (`ADR-0003`) |
| 로그인 | `@LoginUser UserInfo` | 유효한 액세스 토큰이 필요하다. 기본값 |
| 본인 | 서비스에서 판정 | 로그인 + 대상의 소유자여야 한다 (판매자 · 채팅방 참여자). 아니면 403 |

- [G] 경로 기반 공개 설정(`WebConfig.PUBLIC_ENDPOINTS`)에 경로를 더하지 않는다. 지금 있는
  `/api/auth/**` 하나만 둔다. 경로 목록은 HTTP 메서드를 구분하지 못해 같은 경로의 쓰기 API 까지
  열린다 (`ADR-0003`)

## 엔드포인트

요구사항 ID 와 연결한다. **구현 상태는 이 표에 두지 않는다 — `requirements.md` 의 `상태` 열이 정본이다.** 같은 값을 두 곳에 두면 갈라진다(T-38).

| 요구사항 | 메서드 · 경로 | 등급 |
|---|---|---|
| `AU-01` | `POST /api/auth/signup` | 공개 |
| `AU-02` | `POST /api/auth/login` | 공개 |
| `AU-03` | `POST /api/auth/refresh` | 쿠키 |
| `AU-04` | `POST /api/auth/logout` | 쿠키 |
| `PD-01` | `POST /api/products` | 로그인 |
| `PD-02` | `POST /api/products/{id}/images/presign` · `/confirm` · `DELETE /images` | 본인 |
| `PD-03` | `GET /api/products` | 공개 |
| `PD-04` | `GET /api/products/{id}` | 공개 |
| `PD-05` | `PATCH /api/products/{id}` | 본인 |
| `PD-06` | `DELETE /api/products/{id}` | 본인 |
| `PD-07` | `GET` · `POST` · `DELETE /api/products/{id}/like` | 로그인 |
| `PD-08` | `GET /api/products/me/likes` | 로그인 |
| `CH-01` | `POST /api/products/{id}/chat-rooms` | 로그인 |
| `CH-02` | `POST /api/chat-rooms/{id}/messages` | 본인 |
| `CH-03` | `/ws` 로 연결한 뒤 `/topic/room.{id}` 구독 (`ADR-0007`) | 본인 |
| `CH-04` | `GET /api/chat-rooms/{id}/messages` | 본인 |
| `CH-05` | `GET /api/chat-rooms` | 로그인 |
| `CH-06` | `POST /api/chat-rooms/{id}/read` | 본인 |
| `CH-07` | `GET /api/products/{id}/chat-rooms` | 본인 |
| `TR-01` | `POST /api/products/{id}/reservation` | 본인 |
| `TR-02` | `DELETE /api/products/{id}/reservation` | 본인 |
| `TR-03` | `POST /api/products/{id}/completion` | 본인 |
| `TR-04` | `GET /api/products/me/sales` | 로그인 |
| `TR-05` | `GET /api/products/me/purchases` | 로그인 |
| `NT-03` | `GET /api/notifications` · `POST /api/notifications/{id}/read` | 로그인 |

**사라진 엔드포인트** — `PATCH /api/products/{id}/on-sale` 은 지웠다 (등록 즉시 판매중, `T-05` · `PD-01`).
`/api/orders/**` · `/api/payments/**` · `/api/inspections/**` 도 지웠다 (`T-04` · `NG-01` · `NG-02` · `NG-04`)
