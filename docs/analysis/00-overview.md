# 00. 전체 개요 (HonBam-api)

> 본 문서는 `docs/analysis/` 분석 묶음의 진입점이다. 도메인별 상세는 각 `0X-*.md` 참조.

## 1. 프로젝트 정체성

HonBam은 홈텐딩(Home-tending, 홈 칵테일) 문화를 즐기는 사용자들을 위한 소셜 플랫폼이며, 본 리포지토리는 그 **백엔드 API 서버**다. 인증, SNS(게시글/댓글/좋아요/팔로우), 칵테일 레시피, 자유게시판, 실시간 채팅, 알림, 미디어 업로드, Toss 결제/구독을 한 모놀리식 코드베이스에서 제공한다.

- 기술 스택: **Spring Boot 2.7.17 / Java 11** (Docker 빌드는 JDK 17)
- 규모: `src/main/java` 기준 **194개 Java 파일**, 8개 비즈니스 도메인
- 런타임 인프라: MySQL 8 · Redis(Lettuce) · RabbitMQ(인프라만, 미사용) · AWS S3(presigned)

## 2. 기술 스택 / 외부 연동

| 영역 | 사용 기술 |
|------|-----------|
| 웹/REST | Spring MVC, springdoc(Swagger UI) |
| 영속성 | Spring Data JPA, MySQL 8, p6spy(SQL 로깅) |
| 인증/인가 | Spring Security, JWT(jjwt), OAuth2 Client(Kakao/Naver) |
| 실시간 | WebSocket + STOMP(인메모리 SimpleBroker) |
| 캐시/메시징 | Redis(토큰/티켓/Pub-Sub), Spring Cache, (RabbitMQ 의존성 존재·미사용) |
| 비동기 | Spring `ApplicationEventPublisher` + `@Async` + `@TransactionalEventListener` |
| 스토리지 | AWS S3 presigned URL (로컬 `/uploads` 정적 매핑 병행) |
| 결제 | Toss Payments (WebClient/RestTemplate) |
| 외부 HTTP | WebClient(webflux) + RestTemplate 혼용 |

설정/시크릿(`application.yml`, `.env`)은 `.gitignore`로 제외 → 코드가 요구하는 설정 키 전수는 `10-infra-build.md`의 **설정 키 카탈로그** 참조.

## 3. 패키지 맵 (`com.example.HonBam`)

| 패키지 | 책임 | 상세 문서 |
|--------|------|-----------|
| `auth` | JWT/OAuth2 인증, 토큰 발급·회전, WS 티켓 | `01-auth-security.md` |
| `config`, `filter`, `interceptor` | Security/WebSocket/Redis/Async 설정, JWT 필터, STOMP 인터셉터 | `01`, `02` |
| `chatapi` | 실시간 채팅(1:1/그룹/오픈), 이벤트 비동기 | `02-chat-realtime.md` |
| `notification` | 알림 이벤트 체인, Redis→WS 브릿지 | `03-notification.md` |
| `snsapi` | 게시글/댓글/좋아요/팔로우 | `04-sns.md` |
| `recipeapi` | 칵테일 레시피(읽기 전용) | `05-recipe.md` |
| `freeboardapi` | 자유게시판 | `06-freeboard.md` |
| `paymentsapi` | Toss 결제/구독 | `07-payments.md` |
| `upload` | S3 presigned 미디어 업로드 | `08-upload-media.md` |
| `userapi` | 사용자/프로필 | `09-user.md` |
| `redis` | Redis 보조 API/엔티티/서비스 | `02`, `03` 참조 |
| `exception`, `util` | 전역 예외 처리, 쿠키/구독/유저 유틸 | `01` 외 |

엔트리: `HonBamApplication.java`(`@ConfigurationPropertiesScan`, `@EnableScheduling`), SQL 로그 포맷터 `P6SpySqlFormatter.java`.

## 4. 핵심 설계 포인트 (README 의도 ↔ 실제)

1. **WebSocket 인증 분리(티켓)** — HTTP(JWT) 인증 후 1회용 WS 티켓 발급(Redis TTL 30s) → STOMP CONNECT 시 검증. ✅ 구현됨 (`01`, `02`).
2. **이벤트 기반 비동기 처리** — 메시지 INSERT는 핵심 경로만, 브로드캐스트/알림/unread는 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`로 분리. ✅ 구현됨 (`02`).
3. **Redis Pub/Sub 멀티 인스턴스 동기화** — 알림은 `notification:*` 채널로 fan-out 동작 ✅. 단, 채팅 **읽음 동기화**는 구독자만 있고 발행자가 없어 크로스 인스턴스 미완 ⚠️ (`02`, `11`).
4. **RabbitMQ STOMP relay** — 인프라/설정값은 준비됐으나 코드는 `enableSimpleBroker` 사용 → relay 미활성(dead config) ⚠️ (`02`, `10`, `11`).

## 5. 공통 아키텍처 패턴

- **2단계 조회**: ID만 페이징 → fetch join으로 재조회(N+1 회피). 주로 `snsapi`.
- **비정규화 카운터**: `Post.likeCount/commentCount`를 `@Modifying UPDATE`로 원자 증감(동시성 고려).
- **느슨한 User 참조 vs 직접 매핑 혼재**: `snsapi`·`upload`·`payments`는 User를 ID 문자열로 참조, `freeboardapi`는 `@ManyToOne User` 직접 매핑 → 도메인 간 전략 불일치(`11`).
- **이벤트 기반 알림**: 도메인 행위(메시지/팔로우/좋아요) → `ApplicationEventPublisher` → `notification` 리스너.
- **Presigned URL 미디어**: 업로드/조회 모두 S3 presigned, 완료 콜백에서 `headObject` 검증 후 `Media` 영속.

## 6. 테스트 현황

- 테스트는 **채팅 도메인에 집중**(커서 페이징/성능/동시성), Testcontainers(MySQL) 기반.
- SNS/recipe/freeboard/payments/upload/user 등 나머지 도메인 회귀 테스트는 **전무** → 주요 리스크(`11-findings.md`).

## 7. 도메인 횡단 발견사항 (요약 — 상세/등급은 `11-findings.md`)

| # | 항목 | 분류 |
|---|------|------|
| 1 | 채팅 읽음 Redis 동기화 발행자 부재 | 기능 버그 의심 |
| 2 | `ChatReadFlushScheduler` flush 중 키 조기 삭제 의심 | 기능 버그 의심 |
| 3 | JWT 만료 판별 대소문자 불일치(`expired` vs `TOKEN_EXPIRED`) | 기능 버그 의심 |
| 4 | RabbitMQ STOMP relay 설정만 존재·미사용 | 설정-구현 괴리 |
| 5 | User 참조 전략 불일치(ID 참조 vs @ManyToOne) | 일관성 |
| 6 | refresh 해싱 라이브러리 혼용 | 일관성 |
| 7 | 결제 모듈 WebClient/RestTemplate 혼용 | 일관성 |
| 8 | `RecipeRepository` ID 타입 불일치(Integer vs Long) | 타입/안전성 |
| 9 | 도메인 테스트 공백 | 테스트 |

## 8. 문서 인덱스

- `01-auth-security.md` · `02-chat-realtime.md` · `03-notification.md`
- `04-sns.md` · `05-recipe.md` · `06-freeboard.md` · `07-payments.md` · `08-upload-media.md` · `09-user.md`
- `10-infra-build.md` · `11-findings.md`
