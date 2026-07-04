# 11. 도메인 횡단 발견사항 (통합)

각 도메인 문서에서 코드를 재확인하며 등급화한 발견사항을 한곳에 모아 심각도/카테고리로 우선순위화한다. 등급은 **CONFIRMED**(코드 사실로 확정) / **PLAUSIBLE**(영향은 합리적 추론, 런타임 설정·데이터 의존)로 표기한다. 본 단계는 **기록만** 하며 코드 수정은 하지 않는다.

> 참고: `application.yml`/`.env`가 리포에 없어(`.gitignore`) 일부 런타임 영향은 PLAUSIBLE로 남는다.

## A. 기능 버그 의심 (우선 검토 권장)

| ID | 심각도 | 등급 | 발견 | 근거 | 문서 |
|----|--------|------|------|------|------|
| A-1 | HIGH | CONFIRMED(코드)/PLAUSIBLE(영향) | **OAuth2 로그인 시 refresh 쿠키에 원본이 아닌 해시(`refreshHash`)를 저장.** 로컬 로그인은 원본을 넣음. 회전 시 쿠키값을 다시 해싱하므로 소셜 로그인 사용자의 토큰 재발급이 깨질 개연성 | `OAuth2SuccessHandler.java:116` vs 로컬 경로 | 01 |
| A-2 | HIGH | CONFIRMED | **JWT 만료 판별 대소문자 불일치.** `TokenProvider`는 `"TOKEN_EXPIRED"`를 던지나 `JwtExceptionFilter`는 `contains("expired")`(대소문자 구분)로 검사 → 만료가 `ACCESS_TOKEN_EXPIRED`가 아닌 fallback `INVALID_JWT`로 응답. 토큰 타입 메시지도 동일 불일치 | `TokenProvider.java:104`, `JwtExceptionFilter.java:39` | 01 |
| A-3 | HIGH | CONFIRMED | **이메일 중복검사 인자 순서 역전.** `UserService.create`가 `isDuplicate(email, "email")`로 호출하나 분기는 `(type, value)` 기대 → 항상 false 반환, `DuplicateEmailException` 분기가 죽은 코드. 실제 차단은 DB unique 제약에만 의존 | `UserService.java:45` vs `:89-96` | 09 |
| A-4 | MEDIUM | CONFIRMED | **채팅 읽음 크로스 인스턴스 동기화 미완.** `chat:read:event` 구독자(`ChatEventSubscriber`)는 있으나 해당 채널로 발행(`convertAndSend`)하는 코드가 전무 → 멀티 인스턴스 환경에서 읽음 상태가 인스턴스 간 동기화되지 않음(in-process 브로드캐스트만 동작) | `RedisConfig.java:85`, 발행자 부재 | 02 |
| A-5 | MEDIUM | CONFIRMED(구조)/PLAUSIBLE(유실) | **`ChatReadFlushScheduler` flush 루프 내 키 조기 삭제.** messageId 순회 루프 안에서 `redisTemplate.delete(key)`를 호출 → 첫 항목 처리 후 키 전체 삭제, 루프 중 예외 시 미flush id 유실 가능. delete는 루프 밖 1회여야 함 | `ChatReadFlushScheduler.java:61` | 02 |
| A-6 | MEDIUM | CONFIRMED | **구독 만료 스케줄러 사실상 비동작.** 만료 기준일 하드코딩 + 상태 변경 로직 전체 주석 처리 → 구독 만료 처리가 실행되지 않음 | `SubscriptionService.java:47-63` | 07 |
| A-7 | MEDIUM | PLAUSIBLE | **결제 취소 시 신규 INSERT 우려.** `toEntity(user, paidId)`가 paidId를 빌더에 넣지 않아 기존 PaidInfo 갱신이 아닌 새 행 삽입 가능 | `TosspaymentRequestDTO.java:88-100` | 07 |
| A-8 | LOW-MED | PLAUSIBLE | **자유게시판 댓글 목록 DTO 매핑 불일치.** 조회 JPQL의 select 컬럼 순서/타입이 `FreeboardCommentResponseDTO` 생성자와 어긋남 | `FreeboardCommentRepository.java:13` vs `FreeboardCommentResponseDTO.java:10-16` | 06 |
| A-9 | LOW-MED | CONFIRMED | **자유게시판 벌크 JPQL DELETE가 cascade/orphanRemoval 우회** → 연관 댓글 정리 누락 가능 | `FreeboardRepository`(벌크 delete) | 06 |
| A-10 | LOW | CONFIRMED | **팔로우 알림이 멱등 경계 밖에서 발행.** 이미 팔로우 중인데 재요청해도 저장 없이 `FollowerCreatedEvent`가 매번 발행(중복 알림). 좋아요는 반대로 신규 건만 발행 | `FollowService.java:44-49` | 04 |
| A-11 | LOW | PLAUSIBLE | **비정규화 카운터 드리프트.** `removeLike`가 `decreaseLikeCount` 반환값 미검증 + `likeCount>0` 가드로 행 삭제와 카운터 불일치 여지 | `LikeService.java:68` | 04 |
| A-12 | LOW | CONFIRMED | **`LikeStatusResponse.liked`가 취소 시에도 true 하드코딩** | `LikeController.java:40` | 04 |
| A-13 | LOW | CONFIRMED | **알림 읽기/읽음 처리 API 부재.** 알림은 적재·푸시만 되고, 조회/카운트/`markRead()` 호출 경로가 전무(쓰기 전용) | `NotificationRepository` 미사용, 컨트롤러 부재 | 03 |

## B. 설정 ↔ 구현 괴리

| ID | 등급 | 발견 | 근거 | 문서 |
|----|------|------|------|------|
| B-1 | CONFIRMED | **RabbitMQ STOMP relay 설정만 존재·미사용.** `app.rabbitmq.stomp.*` 5개를 `@Value` 주입하나 `configureMessageBroker`는 `enableSimpleBroker`만 사용, `enableStompBrokerRelay` 호출 전무. docker-compose에 RabbitMQ 컨테이너+플러그인까지 떠 있으나 코드는 미연동 → 수평 확장 시 브로커 공유 불가 | `WebSocketConfig.java:39-52, 63-67` | 02, 10 |
| B-2 | CONFIRMED | **MinIO 의존성 미사용.** `io.minio:minio:8.5.1` 선언·`endpointOverride` import 있으나 코드는 AWS S3 SDK만 사용, `io.minio` import 0건 → MinIO 전환 미완 정황 | `build.gradle:77`, `S3Config` | 08, 10 |
| B-3 | CONFIRMED | **Toss 결제 PaymentStatus enum dead.** `PaymentInfo.paymentStatus`는 항상 `PENDING`, `COMPLETED/CANCELED`로 전이하는 코드 없음 | `TossService`, `PaymentInfo` | 07 |
| B-4 | CONFIRMED | **JDK 버전 불일치.** Docker 빌드/런타임은 JDK 17, `build.gradle sourceCompatibility = 11` | `Dockerfile`, `build.gradle` | 10 |
| B-5 | CONFIRMED | **DDL 관리 일원화 부재.** `init` SQL은 `tbl_recipe`만, 나머지는 JPA `ddl-auto` 자동 생성 의존(마이그레이션 도구 없음) | `init/01_schema.sql` | 10 |

## C. 일관성 / 컨벤션

| ID | 등급 | 발견 | 근거 | 문서 |
|----|------|------|------|------|
| C-1 | CONFIRMED | **User 참조 전략 불일치.** SNS·upload·payments는 User를 `String` ID로 느슨 참조, freeboard는 `@ManyToOne User` 직접 매핑 | `Post`(authorId) vs `Freeboard`(@ManyToOne) | 04, 06 |
| C-2 | CONFIRMED | **freeboard 폴더명 케이싱 불일치.** 폴더는 대문자 `Service/`, 패키지 선언은 소문자 `service` → 대소문자 구분 빌드 환경 위험 | `freeboardapi/Service/` | 06 |
| C-3 | CONFIRMED | **결제 HTTP 클라이언트 혼용.** 승인은 `RestTemplate`, 취소·조회는 `WebClient`, 모두 매 호출 `new` 생성 | `TossService.java:117-193` | 07 |
| C-4 | CONFIRMED | **refresh 해싱 라이브러리 죽은 import.** 실제 해싱은 Spring `DigestUtils`로 일원화, `OAuth2SuccessHandler`의 Apache `commons-codec` import는 미사용 | `TokenProvider.java:11,65`, `OAuth2SuccessHandler.java:10` | 01 |
| C-5 | CONFIRMED | **이벤트 필드 명명 혼동.** `LikeCreateEvent`의 첫 필드 `likeId`에 실제로는 좋아요 누른 사용자 ID가 매핑됨 | `LikeCreateEvent` | 03, 04 |
| C-6 | CONFIRMED | **`inviteUser` `@PathVariable` 이름 불일치**(`roomUuId` vs `roomUuid`) / **Direct 방 생성 경로 이원화**(락 경로 vs 무락 경로) | `ChatRoomController`, `ChatRoomService` | 02 |
| C-7 | CONFIRMED | **미사용 자산.** `UserPay` enum 미사용(등급은 `Role`), `completeOne`/`validatePostWriter`·`validateCommentWriter` 데드코드, 프리미엄 승급이 만든 토큰 미사용 | 각 도메인 | 06, 08, 09 |

## D. 타입 / 안전성 / 노출

| ID | 등급 | 발견 | 근거 | 문서 |
|----|------|------|------|------|
| D-1 | CONFIRMED | **`RecipeRepository` ID 타입 불일치.** `JpaRepository<Recipe, Integer>`인데 PK는 `Long dataId`. 현 사용 메서드는 무사하나 `findById` 사용 시 결함 | `RecipeRepository.java:8`, `Recipe.java:18` | 05 |
| D-2 | CONFIRMED | **`RecipeController` 예외 왜곡.** 모든 예외를 `printStackTrace` 후 `400` + `getMessage()` 반환 → 서버 오류도 클라 오류로 표기 | `RecipeController.java:27-30,39-42` | 05 |
| D-3 | CONFIRMED | **엔티티 직접 노출.** 레시피 전량(`findAll`, 페이징 없음), 팔로워/팔로잉 목록을 인증 없이 엔티티째 노출 | `RecipeController`, `FollowController` | 04, 05 |
| D-4 | PLAUSIBLE | **업로드 정책 검증이 사후.** `/complete`의 `headObject` contentType 검증이 S3 업로드 이후라 잘못된 타입도 객체는 잔존(고아 객체). CHAT은 검증 자체 없음 | `UploadService.java:60` | 08 |
| D-5 | PLAUSIBLE | **사용자 탈퇴 시 연관 정리 미흡.** `UserProfileMedia`는 역방향 cascade 없고 `user_id` not-null FK → 프로필 보유 사용자 삭제 시 FK 위반 가능, Media·S3 정리 부재 | `UserService`(delete) | 09 |
| D-6 | CONFIRMED | **알림 발행/영속 트랜잭션 경계 취약.** `publishAndPersist`가 `@Transactional` 없이 Redis 발행(먼저)→DB save(나중)를 try/catch로 흡수 → "푸시됐지만 DB 미기록" 비정합 가능 | `NotificationPublisher`(L30/L39) | 03 |

## E. 테스트 / 품질 게이트

| ID | 등급 | 발견 | 문서 |
|----|------|------|------|
| E-1 | CONFIRMED | **도메인 테스트 공백.** 테스트는 채팅(커서/성능/동시성)에만 존재. SNS/recipe/freeboard/payments/upload/user 회귀 테스트 전무. Docker 빌드도 `-x test`로 테스트 제외 | 10 |
| E-2 | CONFIRMED | **N+1 잠재.** SNS 2단계 페이징이 `getMyFeeds/getExplorePosts/getTodayShots`에만 적용, `getFeedPosts`/`getUserPosts`·댓글 작성자 조회·읽음 처리 참여자별 unread 쿼리에 N+1 여지 | 02, 04 |

## 권장 처리 순서 (제안)

1. **즉시 검증 대상(인증/결제 정확성)**: A-1, A-2, A-3, A-6 — 사용자 로그인/결제·구독에 직접 영향.
2. **멀티 인스턴스 전제 점검**: A-4, A-5, B-1 — 단일 인스턴스에서는 드러나지 않으나 확장 시 정합성 붕괴.
3. **데이터 정합성**: A-7, A-9, A-11, D-5, D-6 — 카운터/연관/결제 행 일관성.
4. **정리/일관성**: B-2~B-5, C-*, D-1~D-4 — 리팩토링/컨벤션.
5. **품질 게이트 구축**: E-1, E-2 — 회귀 테스트·쿼리 점검을 후속 작업 기반으로.

> 수정 착수 전 각 항목의 PLAUSIBLE 등급은 런타임/설정으로 재현 확인 후 진행 권장. 실제 코드 변경은 별도 승인 작업으로 분리한다.
