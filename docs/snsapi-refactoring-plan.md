# SNS 도메인(`snsapi`) 리팩토링 계획

## Context

`refactor/snsapi` 브랜치에서 SNS 도메인(게시물/댓글/좋아요/팔로우, 31개 파일 ~1,800줄)을 리팩토링한다. 전수 분석 결과 실제 동작 오류(사용자가 오류를 확인한 today-shots 쿼리, 좋아요 취소 응답 오류, NPE 경로, 카운트 desync, 중복 알림)와 N+1 문제, 예외 처리 불일치가 확인됐다. 테스트는 0개다.

**우선순위**: ① 버그 수정 → ② 성능 개선 → ③ 구조/가독성. payments 리팩토링(커밋 54c8b7b)의 관례(도메인 예외 + GlobalExceptionHandler, DTO 정적 팩토리, Mockito 단위테스트)를 따른다.

**사용자 확정 정책**:
- 게시물 삭제 시 댓글/좋아요 고아 데이터: **현행 유지** (이번에 건드리지 않음, 후속 과제로만 기록)
- 부모 댓글 삭제: **soft delete** — 대댓글 유지, 서버가 content를 `"삭제된 댓글입니다."` 대체 문구로 반환
- 권한 예외: **신규 도메인 예외 + 403 Forbidden** 통일
- API breaking change **허용**: 팔로워/팔로잉 목록을 DTO + **페이징**으로 개선, 좋아요 취소 응답 `liked=false` 수정
- 테스트: **버그 수정 부분만** 단위테스트 (PaymentServiceTest의 Mockito 패턴)

**API 호환성 원칙**: URL 경로(`/api/sns/**`)는 전부 유지. 응답 변경은 위 승인 항목만.

**외부 영향 없음**: snsapi 클래스를 패키지 밖에서 import하는 코드는 0건 (의존 방향은 snsapi → upload/userapi/auth/notification 단방향).

---

## 1단계 — 예외 통일 (독립)

`exception/` 패키지에 신규:
- `SnsAccessDeniedException` → 403 `SNS_ACCESS_DENIED` (게시물/댓글/미디어 소유권 위반 공용)
- `InvalidCommentException` → 400 `INVALID_COMMENT` (대댓글에 답글 작성, 삭제된 댓글에 답글/수정 등)

`GlobalExceptionHandler.java`에 두 핸들러 추가 (payments 항목들과 동일한 `ErrorResponse` 형식).

교체 대상:
- `PostService.deletePost:244`, `CommentService.updateComment:90`, `deleteComment:104` — `SecurityException` → `SnsAccessDeniedException`
- `PostService.updatePost:187`, `createPost:130`, `updatePost:201` — 소유권 위반 `CustomUnauthorizedException` → `SnsAccessDeniedException`
- `CommentService.createComment:53` — `RuntimeException("대댓글에 댓글을 작성할 수 없습니다")` → `InvalidCommentException`

## 2단계 — 버그 수정 (핵심)

### 2-1. today-shots 쿼리 오류 (사용자가 오류 확인 — 최우선)
`PostRepository.findTodayShotIds:66-75` — `SELECT DISTINCT p.id` + SELECT에 없는 컬럼(`likeCount`, `createdAt`) ORDER BY는 MySQL에서 `ER_FIELD_IN_ORDER_NOT_SELECT` 오류. **JOIN+DISTINCT를 EXISTS로 교체**:
```jpql
SELECT p.id FROM Post p
WHERE p.createdAt BETWEEN :start AND :end
  AND EXISTS (SELECT 1 FROM PostMedia pm WHERE pm.post = p)
ORDER BY p.likeCount DESC, p.createdAt DESC
```
중복 행이 없어지므로 DISTINCT 자체가 불필요해진다.

### 2-2. 좋아요 취소 응답 `liked=true` 오류
`LikeController.removeLike:40` — `new LikeStatusResponse(true, likeCount)` → `false`.

### 2-3. getTodayShots NPE
`PostService.getTodayShots:402` — 작성자가 탈퇴한 게시물이면 `authorMap.get(...)`이 null인데 null 체크(`buildTodayShotDTO` 내부) **이전에** `author.getId()` 호출 → NPE. author null 체크를 스트림 매핑 첫 줄로 이동.

### 2-4. 댓글 soft delete (대댓글 고아 + commentCount desync 해결)
- `Comment` 엔티티에 `boolean deleted` 필드 추가 (기본 false). **⚠️ 스키마**: `sns_comment`에 컬럼 추가 필요 — application.yml이 레포에 없어 ddl-auto 확인 불가. `update`가 아니면 수동 `ALTER TABLE sns_comment ADD COLUMN deleted BIT(1) NOT NULL DEFAULT 0` 실행 필요(실행 전 사용자 확인).
- `CommentService.deleteComment` 정책:
  - 대댓글 존재(`commentRepository.existsByParentId(commentId)` 신규) → `comment.markDeleted()` soft delete
  - 대댓글 없음 → 기존대로 hard delete
  - 두 경우 모두 `decreaseCommentCount` 1회 (현행 유지)
  - 이미 `deleted`인 댓글 재삭제 요청 → `CommentNotFoundException`
- 응답: `convertToCommentDTO`에서 `deleted`면 content를 `"삭제된 댓글입니다."`로 대체 (사용자 선택: 서버 대체 문구)
- 가드: 삭제된 댓글에 답글 작성(`createComment`의 parent 체크) 및 수정(`updateComment`) → `InvalidCommentException`

### 2-5. 댓글 수정/삭제가 URL의 `{postId}`를 무시
`CommentService.updateComment:83`, `deleteComment:100` — `findById(commentId)` → `findByIdAndPostId(commentId, postId)` (이미 존재하는 리포지토리 메서드 재사용, `SnsCommentController`에서 postId 전달).

### 2-6. 중복 팔로우 시 알림 이벤트 중복 발행
`FollowService.follow:44-49` — `publishEvent`를 `if (!existsById)` 블록 **안으로** 이동 (신규 팔로우일 때만 알림).

### 2-7. 팔로우 대상 존재 검증 누락
`FollowService.follow` — 존재하지 않는 userId도 팔로우 가능. `userRepository.existsById(targetId)` 검증 추가 → `UserNotFoundException`(404, 핸들러 기존재).

### 2-8. 좋아요/팔로우 check-then-act 레이스
`LikeService.addLike:31-35`, `FollowService.follow:44-45` — 동시 요청 시 둘 다 exists 통과 → PK 충돌로 500. `save`를 try-catch로 감싸 `DataIntegrityViolationException` 발생 시 멱등 처리(조용히 return, 카운트 증가·이벤트 발행 생략). addLike는 post 조회를 메서드 최상단으로 이동해 `findById` 1회로 존재 검증 + authorId 확보(기존 `updated==0` 체크 + 별도 findById 중복 제거).

## 3단계 — 팔로워/팔로잉 목록 DTO + 페이징 (breaking, 승인됨)

- 신규 `FollowUserResponseDTO` (`userId`, `nickname`, `profileImageUrl`, `followedAt`) + `static from(...)`
- `FollowRepository`: `Page<Follow> findAllByIdFollowingId(String, Pageable)` / `findAllByIdFollowerId(String, Pageable)` 로 교체
- `FollowService.getFollowers/getFollowing(userId, page, size)`: Follow 페이지 조회 → 상대 userId 수집 → `userRepository.findAllById` + `userProfileMediaRepository.findByUser_IdIn` **일괄 조회** (PostService.convertToDTOList:255-308의 배치 패턴 재사용) → DTO 매핑
- `FollowController.getFollowers/getFollowing`에 `page`/`size` 파라미터 추가(기본 0/20), 엔티티 직접 반환 제거

## 4단계 — 성능 개선

### 4-1. 피드/유저 게시물 N+1
`PostService.getFeedPosts:104-107`, `getUserPosts:232-235` — media fetch join 없이 조회 후 `convertToDTOList`에서 게시물마다 lazy 로딩. `getMyFeeds:56-79`와 동일한 **id 페이징 → `findAllWithMediaByIdIn` fetch join → 순서 재정렬** 패턴으로 교체:
- `findFeedPosts` → `SELECT p.id ...`로 변경한 `findFeedPostIds` 신규
- `findByAuthorIdOrderByCreatedAtDesc` → 기존 `findPostIdsByAuthorId` 재사용 (getUserPosts와 getMyFeeds가 사실상 동일 로직이 되므로 private 헬퍼로 통합)
- "id 조회 → fetch join → 재정렬" 3중 중복(getMyFeeds/getExplorePosts/getTodayShots)도 같은 헬퍼로 통합

### 4-2. 댓글 조회 N+1
`CommentService.convertToCommentDTO:159-167` — 댓글마다 `userRepository.findById` + `userProfileMediaRepository.findByUser` 호출 (댓글 N개 = 2N 쿼리). `getComments`/`getReplies`에서 작성자 id 수집 → `findAllById` + `findByUser_IdIn` 일괄 조회 → Map 기반 매핑으로 변경. 작성자가 없는 댓글은 예외 대신 스킵 또는 닉네임 null 처리(PostService.convertToDTOList:292-296과 동일하게 log.warn 후 skip).

### 4-3. 트랜잭션 정리
- `FollowService` 클래스 레벨 `@Transactional`(read-write) 제거 → 쓰기 메서드(`follow`/`unFollow`)에만 `@Transactional`, 조회는 `@Transactional(readOnly = true)`
- `PostRepository`의 4개 카운트 UPDATE 쿼리 `@Modifying` 옵션 통일: 전부 `clearAutomatically = true` (현재 decrease만 지정된 비대칭 해소)

## 5단계 — 구조/가독성 정리

- **중복 헬퍼 통합**: `resolveAuthorProfileUrl`이 PostService/CommentService/FollowService에 3중 복제 → `snsapi/service/support/AuthorProfileResolver`(신규 @Component)로 추출: 단건 `resolve(User)` + 일괄 `resolveKeyMap(Set<String> authorIds)` 제공. `getAuthor` 중복도 함께 정리.
- **미사용 코드 삭제**:
  - `CommentRepository:5` — `JaasPasswordCallbackHandler` 오타성 import
  - `FollowRepository`/`PostLikeRepository` — JpaRepository 기본 제공과 중복인 `existsById`/`deleteById` 오버라이드, 미사용 `deleteByIdCustom`
  - `CommentRepository` — 미사용 `findByPostIdOrderByIdAsc`, `countByPostId` (삭제 전 usage 재확인)
  - `PostRepository.findByAuthorIdInOrderByIdDesc` — 4-1 이후 미사용이면 삭제
  - response DTO에 잘못 붙은 `@Service`/import (`PostListResponseDTO`, `TodayShotResponseDTO`), `PostListResponseDTO` 자체가 미사용이면 삭제
  - `LikeService.removeLike:68` — 미사용 지역변수 `updated`
- `PostController.deletePost:103-109` — `void` → `ResponseEntity<Void>` (204 No Content)
- 응답 래핑 일관화: `Map.of(...)` 즉석 응답(checkLiked, getLikeCount 등)은 기존 `LikeStatusResponse`/`FollowStatusResponse` DTO 재사용으로 통일 (JSON 필드명 유지)

## 6단계 — 테스트 (버그 수정 부분만, PaymentServiceTest 패턴)

`src/test/java/com/example/HonBam/snsapi/` 신규, `@ExtendWith(MockitoExtension.class)` + BDDMockito + AssertJ:
- `CommentServiceTest`: 대댓글 有→soft delete + 대체 문구, 無→hard delete, 삭제된 댓글에 답글/수정/재삭제 차단, postId 불일치 시 `CommentNotFoundException`, 타인 댓글 수정 시 `SnsAccessDeniedException`
- `FollowServiceTest`: 중복 팔로우 시 이벤트 미발행(`then(eventPublisher).should(never())`), 신규 팔로우 시 1회 발행, 자기 자신 팔로우 차단, 미존재 대상 `UserNotFoundException`
- `LikeServiceTest`: addLike 멱등(중복 시 카운트 미증가·이벤트 미발행), removeLike 후 상태
- `PostServiceTest`: getTodayShots에서 작성자 null 게시물 스킵(NPE 없음)
- `PostRepositoryTest` (**Testcontainers MySQL**, 기존 `common/TestContainerConfig` 상속): `findTodayShotIds`가 실제 MySQL에서 오류 없이 실행되고 likeCount 내림차순 정렬됨을 검증 — 2-1 수정의 핵심 검증

## 문서화 (사용자 요청)

이 계획서를 `docs/snsapi-refactoring-plan.md`로 저장 (기존 `docs/payment-refactoring-plan.md`와 동일 형식). 후속 과제 섹션 포함: 게시물 삭제 시 댓글/좋아요/Media 정리(이번 범위 제외), 팔로우 목록 무한스크롤 커서 페이징, likeCount 정합성 배치 보정.

## 검증

1. `./gradlew compileJava compileTestJava` — 단계별 컴파일 유지
2. `./gradlew test` — 신규 테스트 + 기존 테스트(payments/chat) 전부 통과. Testcontainers MySQL로 `findTodayShotIds` 실행 검증이 핵심
3. 수동 확인(서버 기동 가능 시): `GET /api/sns/feed/today-shots`(오류 재현→해소), `DELETE /api/sns/posts/{id}/like` 응답 `liked=false`, 대댓글 있는 댓글 삭제 후 `GET .../comments`에 "삭제된 댓글입니다." 표시, `GET /api/sns/users/{id}/followers?page=0&size=20` 닉네임/프로필 포함 응답
4. `deleted` 컬럼: ddl-auto가 `update`가 아닌 환경이면 배포 전 수동 ALTER 필요 — 사용자에게 운영 DB 반영 방식 확인
