# Chat 도메인 리팩토링 계획 (refactor/chat)

## Context

chat 도메인(`com.example.HonBam.chatapi`, 총 ~2,100줄)의 종합 리팩토링. 특정 장애보다는 구조 정리 + 성능 개선 + 발견된 버그 수정이 목적.

**확정 결정사항:**
- 읽음 모델: **커서 단일화** — `ChatRoomUser.lastReadMessageId`만 사용, `ChatRead` 테이블/스케줄러/Redis 임시저장 전부 삭제
- 멀티 인스턴스 대응: **Redis pub/sub 팬아웃** (RabbitMQ relay 설정 코드는 제거)
- 구조: 서비스 분리 중심, 패키지명 `chatapi` 유지
- sender 비정규화(senderId/senderName 문자열)는 현재 방식 유지
- DB 스키마 변경 허용, 운영 데이터 없음(개발 데이터뿐 — 마이그레이션 스크립트 불필요, 수동 DROP 가능)
- 테스트는 핵심 로직만 최소한

**탐색으로 확인된 주요 문제:**
- `ChatRoomService` 500줄 god class (방 CRUD + 초대 + 읽음 처리 + 브로드캐스트 혼재)
- 읽음 상태 이중 모델 (`ChatRead` 행 + `lastReadMessageId` 커서)
- 실시간 전달이 in-memory SimpleBroker만 사용 — 멀티 인스턴스에서 크로스 노드 전달 안 됨 (`ChatEventSubscriber`는 구독만 있고 발행자 없음, RabbitMQ relay는 설정 주입만 되고 미사용)
- N+1 다수: `roomList`(방마다 2쿼리), `markMessagesAsReadInternal`(참여자마다 count), `findOpenRooms`, `mapMessageWithDetails`(Media lazy)
- DTO 조립 로직 중복: `ChatMessageService.convertToDTO` ↔ `ChatMessageAsyncHandler`

**확인된 버그 (이번에 수정):**
1. `ChatRoomController.inviteUser`: 경로 `/{roomUuId}` vs `@PathVariable roomUuid` 대소문자 불일치 → 바인딩 실패
2. `ChatReadFlushScheduler`: Redis 키를 루프 안에서 삭제 → 읽음 데이터 유실 (커서 단일화로 스케줄러 자체 삭제됨)
3. 대형 방(>10명) 읽음 처리 시 마지막 메시지 1건만 저장 → 소형 방과 불일치 (동일하게 삭제로 해소)
4. `markMessageAsRead`가 본인 메시지면 early-return → 커서가 안 나가는 버그
5. `createRoom`: `participantIds` null 체크 전에 `.contains()` 호출 → NPE
6. `startDirectChat`: `direct=true` 미설정 + `findDirectChatRoom`이 direct 필터 없이 두 유저 포함 아무 방이나 매칭
7. `fileName`에 S3 fileKey 노출 (Media에 원본 파일명 필드 없음)
8. `getMessagesCursor`가 readOnly 아님, 예외 전략 불일치 (raw RuntimeException vs 도메인 예외)

---

## 목표 구조

```
chatapi/
├── api/            ChatRoomController(경로 수정), ChatMessageController(UserRepository 제거), WsTicketController(유지)
├── broadcast/      [NEW] RedisChatPublisher, ChatRedisSubscriber, ChatStompBroadcaster(← component/ChatEventBroadcaster 이동·개명)
├── mapper/         [NEW] ChatMessageMapper, ChatRoomMapper
├── service/        ChatRoomService(슬림화: 방 생성/초대/조인만)
│                   ChatRoomQueryService [NEW] (roomList, findOpenRooms — readOnly)
│                   ChatReadService [NEW] (커서 읽음 처리)
│                   ChatMessageService, MessageSaveCoreService(ChatRoomRepository 직접 의존으로 변경)
│                   ChatRoomUpdateService(updateLastMessage → updateLastMessagePreview 개명)
├── dto/            RoomSummaryEvent [NEW], RoomUnreadCountProjection [NEW], OpenRoomProjection [NEW]
├── event/          ChatReadStateChangedEvent [NEW]
└── 삭제:           entity/ChatRead, ChatReadId · repository/ChatReadRepository · scheduler/ 전체
                    listener/ChatEventSubscriber · dto/request/InviteRequest(미사용)
                    ChatRoomService.joinOpenRoom, markAllAsReadOnJoin (데드 코드 — 호출자 없음 검증됨)

기타: config/WebSocketConfig(RabbitMQ @Value 5개 + 미사용 TokenProvider 제거)
     config/RedisConfig(구독 채널 교체), config/AsyncConfig(AsyncConfigurer 구현 + 예외 핸들러)
     notification/event/ChatMessageCreateEvent(chatapi 엔티티 import 제거, primitive 필드로)
     upload/entity/Media(originalFileName 컬럼 추가), UploadCompleteRequest, UploadService
     build.gradle(spring-boot-starter-amqp 제거 — 다른 사용처 없음 검증됨)
```

---

## Phase 1 — 버그 수정 + 데드 코드 제거 (동작 재설계 없음)

1. `ChatRoomController`: `/{roomUuId}/invite` → `/{roomUuid}/invite` + 명시적 `@PathVariable("roomUuid")`
2. 예외 통일: 방 조회 → `ChatRoomNotFoundException`(기존, GlobalExceptionHandler 매핑 있음), 새 `ChatMessageNotFoundException` 추가(404), `MessageSaveCoreService`의 IllegalArgumentException 3곳 → `MessageSendException`(기존 400 매핑)
3. `getMessagesCursor` → `readOnly = true`; `getMessagesByRoom`은 `getMessagesCursor(roomUuid, null, 50)` 위임; 주석 처리된 `/page` 엔드포인트 삭제
4. `ChatMessageController.sendMessage`: `UserRepository`/`SimpMessagingTemplate` 주입 제거, 닉네임 조회를 서비스로 이동 (`saveMessage(request, userId)` 시그니처)
5. `createRoom` NPE 수정; `startDirectChat`을 direct 경로 위임으로 재구현(`direct=true` 설정, `findDirectChatRoom` 삭제, 기존 `findDirectRoom` + `findByIdWithLock` 락 로직 유지)
6. 데드 코드 삭제: `joinOpenRoom`, `markAllAsReadOnJoin`, `InviteRequest`, 미사용 repo 메서드(`findByRoomId`, `updateLastReadMessageId`, `findTopByRoomIdOrderByIdDesc`, `findByRoomIdOrderByTimestampDesc`), 미사용 import(`Loader`, `GetMapping` 등)
   - ⚠️ `countByRoomId`는 기존 테스트(`src/test/.../Chat/room/`)가 호출 — 테스트 수정과 함께 삭제하거나 유지
7. `WebSocketConfig`: RabbitMQ `@Value` 5개 + `TokenProvider` 제거; build.gradle에서 `spring-boot-starter-amqp` 제거
8. `ChatMessageCreateEvent`: `ChatMessage` 엔티티 대신 primitive 필드 팩토리로 (notification→chatapi 역의존 해소); `ChatMessageAsyncHandler`의 호출부 수정
9. Media 원본 파일명: `Media.originalFileName`(nullable) 추가, `UploadCompleteRequest.fileName` 추가, `UploadService`에서 저장. 매퍼에서 `originalFileName` → 없으면 fileKey의 basename 폴백 (S3 키 원문 노출 금지)

**게이트:** `./gradlew compileJava` + 부트 스모크. 기존 API 동작 동일.

## Phase 2 — Redis pub/sub 팬아웃

채널 → 페이로드 → STOMP 릴레이:

| Redis 채널 | 페이로드 | 로컬 STOMP 전달 |
|---|---|---|
| `chat:broadcast:message` | `ChatMessageResponseDTO` | `/topic/chat.room.{roomUuid}` `{type:"MESSAGE", body}` |
| `chat:broadcast:read` | `ChatReadEvent` | `/topic/chat.room.{roomUuid}.read` `{type:"READ_UPDATE", body}` |
| `chat:broadcast:summary` | `RoomSummaryEvent{userId, roomUuid, unreadCount}` | `/topic/chat.summary.{userId}` `{type:"ROOM_SUMMARY_UPDATE", body}` |

1. `RedisChatPublisher` (기존 `NotificationPublisher` 패턴 따라): Spring `ObjectMapper`(JavaTimeModule 포함) 주입 + `convertAndSend(channel, json)`. `publishMessage/publishRead/publishSummary`
2. `ChatRedisSubscriber`: `RedisConfig`에 `PatternTopic("chat:broadcast:*")` 등록(기존 `chat:read:event` 등록 교체), 채널 접미사로 분기, `ChatStompBroadcaster` 호출. 직접 만든 `new ObjectMapper()` 금지 — Spring 것 주입 (LocalDateTime 직렬화)
3. `ChatEventBroadcaster` → `broadcast/ChatStompBroadcaster` 이동·개명. `broadcastRoomSummaryForParticipants` 삭제(루프는 `ChatMessageAsyncHandler`에서 `publishSummary` 반복 호출로)
4. **원칙: 프로듀서는 Redis에만 발행, 로컬 STOMP 전송은 구독자만.** 발행 인스턴스도 자기 발행을 구독으로 받아 릴레이 → 이중 전송 없음
5. 직렬화 준비: `ChatMessageResponseDTO.FileInfoDTO`에 `@NoArgsConstructor @AllArgsConstructor` 추가 (현재 builder-only라 역직렬화 불가)

**게이트:** 브라우저 탭 2개로 메시지 전송 — 클라이언트당 MESSAGE 프레임 정확히 1개, summary 프레임 도착. `{type, body}` 봉투와 토픽 경로는 프론트 계약이므로 그대로 유지.

## Phase 3 — 커서 단일 읽음 모델

`ChatReadService.markRead(roomUuid, userId, messageId)` — 기존 `updateLastMessage`/`markMessageAsRead`/`markMessagesAsReadInternal` 3단 체인 대체:

1. 방 조회(`ChatRoomNotFoundException`) → **멤버십 체크 추가**(`ChatRoomAccessException` — 현재 없음, 누구나 읽음 처리 가능한 상태) → 메시지 존재+방 소속 검증
2. 본인 메시지여도 커서 전진 (버그 4 수정 — sender 제외는 count 쿼리가 처리)
3. `updateLastReadMessageIdIfNewer` 반환형 `void → int`; 0이면 조용히 반환 (멱등, 중복 브로드캐스트 억제)
4. 커밋 후: `ChatReadStateChangedEvent` → `@TransactionalEventListener(AFTER_COMMIT)` 릴레이가 `publishRead` + `publishSummary`(리더 본인 것만 — 기존의 전 참여자 unread 재계산 루프 제거, 읽음은 리더의 summary만 바꾸므로)
5. 삭제: `ChatRead`, `ChatReadId`, `ChatReadRepository`, `ChatReadFlushScheduler`, `chat:read:temp:*` 및 `chat:read:{roomId}` 해시 코드 (해시는 쓰기만 있고 읽는 곳 없음 — 검증됨)
6. dev DB에서 수동 `DROP TABLE chat_read` (ddl-auto는 drop 안 함)

**게이트:** 읽음 처리 멱등성, `.read` 토픽 페이로드 형식 불변.

## Phase 4 — 서비스 분해 + N+1 해소

**분해 (메서드 배치):**
- `ChatRoomService`(유지): `findByRoomUuid`(예외를 `ChatRoomNotFoundException`으로), `createRoom`, `checkDirectRoomExistence`, `inviteUser`, `startDirectChat`, `joinRoom`
- `ChatRoomQueryService`(NEW, readOnly): `roomList`, `findOpenRooms` (roomList의 불필요한 `userRepository.findById` 존재 검증 제거)
- `ChatRoomMapper`(NEW): `toChatRoomResponse`(현재 4곳 중복 빌더 통합), `toRoomListDTO`, `toOpenRoomDTO`, `resolveDisplayName`
- `ChatMessageMapper`(NEW): `toResponseDTO`, `toFileInfoDTOs` — `ChatMessageService.convertToDTO`와 `ChatMessageAsyncHandler`의 중복 조립 단일화
- `MessageSaveCoreService`: `ChatRoomService` 의존 → `ChatRoomRepository` 직접 (서비스 간 순환 제거)
- `ChatRoomUpdateService.updateLastMessage` → `updateLastMessagePreview` 개명 (읽음 처리 메서드와의 이름 충돌 해소)

**쿼리 (N+1):**
- [추가] `ChatRoomUserRepository.countUnreadPerRoomForUser(userId)` — `LEFT JOIN chat_message m ON m.id > COALESCE(cru.last_read_message_id, 0) AND m.sender_id <> cru.user_id ... GROUP BY room_id` → `roomList`가 방 수와 무관하게 2쿼리
- `roomList`의 마지막 메시지는 `ChatRoom`의 비정규화 컬럼(lastMessage/lastMessageTime/lastMessageId) 사용 → 방마다 `findTopByRoomOrderByTimestampDesc` 제거
- [추가] `ChatRoomRepository.findOpenRoomsWithCount(keyword)` — participants COUNT 포함 projection → lazy `size()` 제거, `findByOpenTrue`/`searchOpenRooms` 대체
- [추가] `ChatMediaRepository.findByMessageInWithMedia` — `join fetch cm.media` → `mapMessageWithDetails`의 Media N+1 해소
- [수정] `countUnreadUsersForMessages`에 `AND m.id IN (:messageIds)` 추가 (페이지 범위로 한정)
- [재사용] `countUnreadUsersForMessage`, `countUnreadMessagesForEachUser`, `countUnreadMessages`, `findDirectRoom`, `findWithUserByRoomId`
- [삭제] 위 대체로 미사용이 된 repo 메서드들 (`countByRoomIdAndSenderIdNot`, `findMessageWithPaging`, `findTopByRoomOrderByTimestampDesc`, `countUnreadMessagesForRoomAndUser`, `findByRoom` 등 — 삭제 전 호출자 재확인)

**게이트:** hibernate SQL 로그에서 `GET /api/chat/rooms` 쿼리 수가 방 개수와 무관하게 일정.

## Phase 5 — 최소 테스트

기존 인프라 활용: Testcontainers MySQL + `TestContainerConfig`, 기존 테스트 위치 `src/test/java/com/example/HonBam/Chat/room/`

1. `ChatReadServiceTest`: 커서 전진 / 과거 messageId no-op / 본인 메시지도 커서 전진 / 비참여자 → `ChatRoomAccessException` / 3인 방 unReadUserCount 정확성 (RedisChatPublisher는 mock)
2. `UnreadCountQueryTest`: `countUnreadPerRoomForUser`(null 커서·중간 커서·최신 커서), `updateLastReadMessageIdIfNewer` 반환값 1/0
3. `MessageSaveCoreServiceTest`: 저장 + ChatMedia 링크 / 타인 미디어 거부 / CHAT 아닌 purpose 거부
4. `ChatBroadcastSerializationTest`: publisher/subscriber와 동일한 ObjectMapper로 DTO 왕복 직렬화 (LocalDateTime, MessageType enum 포함)
5. 기존 `ChatPerformanceTest`, `ChatRoomConcurrencyTest`, `ChatMessageCursorPreTest` 컴파일 확인 — 삭제 메서드(`countByRoomId`) / 변경 시그니처(`saveMessage`) 참조 시 수정

## 검증 방법

- 각 Phase 경계에서 `./gradlew compileJava` (전체 빌드는 `./gradlew build -x test` 후 Phase 5에서 테스트)
- docker-compose로 MySQL/Redis 기동 후 부트 스모크: 방 생성 → 메시지 전송(STOMP) → 읽음 처리 → roomList 조회
- Phase 2 이후: 앱 인스턴스 2개(포트 다르게) 띄워 크로스 인스턴스 메시지 전달 확인 — 이번 리팩토링의 핵심 검증
- SQL 로그로 N+1 제거 확인

## 리스크 / 주의

- **프론트 와이어 계약 불변**: STOMP 토픽 경로와 `{type, body}` 봉투, `POST /api/chat/rooms/read` 요청 형식 유지
- application.yml이 gitignore됨 — `ddl-auto` 값을 로컬에서 확인 후 진행 (update 가정). `media.original_file_name`은 자동 추가되지만 `chat_read`는 수동 drop
- Redis pub/sub은 fire-and-forget — 인스턴스의 Redis 단절 중 이벤트는 유실 (SimpleBroker 대비 회귀 아님, 재연결 시 refetch는 기존 프론트 동작)
- `checkDirectRoomExistence`의 비관적 락(`findByIdWithLock`)은 유지 — 동시 direct 방 생성 가드 (기존 동시성 테스트 존재)
- roomList 응답의 방 이름 필드를 `resolveDisplayName`으로 바꿀 경우 프론트가 직접 상대 이름을 계산하고 있지 않은지 확인
