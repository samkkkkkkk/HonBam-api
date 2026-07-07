# SNS 도메인(snsapi) 리팩토링 작업 체크리스트

> 원본 계획: [snsapi-refactoring-plan.md](./snsapi-refactoring-plan.md)
> 각 단계 완료 시 체크박스에 `x`를 표시하고, 단계 끝의 컴파일 검증까지 통과한 뒤 다음 단계로 진행한다.

## 1단계 — 예외 통일 (독립)

- [x] `SnsAccessDeniedException` 생성 (403, 게시물/댓글/미디어 소유권 위반 공용)
- [x] `InvalidCommentException` 생성 (400, 대댓글에 답글·삭제된 댓글에 답글/수정 등)
- [x] `GlobalExceptionHandler`에 `SnsAccessDeniedException` → 403 `SNS_ACCESS_DENIED` 핸들러 추가
- [x] `GlobalExceptionHandler`에 `InvalidCommentException` → 400 `INVALID_COMMENT` 핸들러 추가
- [x] `PostService.deletePost`: `SecurityException` → `SnsAccessDeniedException`
- [x] `CommentService.updateComment` / `deleteComment`: `SecurityException` → `SnsAccessDeniedException`
- [x] `PostService.createPost` / `updatePost`: 소유권 위반 `CustomUnauthorizedException` → `SnsAccessDeniedException` (미디어 소유권 포함)
- [x] `CommentService.createComment`: `RuntimeException("대댓글에 댓글을 작성할 수 없습니다")` → `InvalidCommentException`
- [x] `./gradlew compileJava compileTestJava` 통과

## 2단계 — 버그 수정 (핵심)

### 2-1. today-shots 쿼리 오류 (최우선 — 오류 확인됨)

- [x] `PostRepository.findTodayShotIds`: `SELECT DISTINCT p.id` + JOIN → `EXISTS` 서브쿼리로 교체 (SELECT에 없는 컬럼 ORDER BY로 인한 MySQL `ER_FIELD_IN_ORDER_NOT_SELECT` 해소)

### 2-2. 좋아요 취소 응답 오류

- [x] `LikeController.removeLike`: `new LikeStatusResponse(true, ...)` → `false`

### 2-3. getTodayShots NPE

- [x] `PostService.getTodayShots`: author null 체크를 `author.getId()` 호출 이전(스트림 매핑 첫 줄)으로 이동 (탈퇴 작성자 게시물 NPE 방지)

### 2-4. 댓글 soft delete (대댓글 고아 + commentCount desync 해결)

- [x] `Comment` 엔티티에 `boolean deleted` 필드 추가 (기본 false) + `markDeleted()` 메서드
- [x] `CommentRepository.existsByParentId(Long parentId)` 신규
- [x] `deleteComment`: 대댓글 존재 → soft delete / 없음 → hard delete / 둘 다 `decreaseCommentCount` 1회
- [x] `deleteComment`: 이미 `deleted`인 댓글 재삭제 → `CommentNotFoundException`
- [x] `convertToCommentDTO`: `deleted`면 content를 `"삭제된 댓글입니다."` 대체 문구로 반환 + 응답에 `deleted` 필드 추가
- [x] `createComment`: 삭제된 부모 댓글에 답글 작성 차단 → `InvalidCommentException`
- [x] `updateComment`: 삭제된 댓글 수정 차단 → `InvalidCommentException`
- [ ] ⚠️ 스키마: `sns_comment`에 `deleted` 컬럼 추가 — ddl-auto가 `update`가 아니면 수동 `ALTER TABLE sns_comment ADD COLUMN deleted BIT(1) NOT NULL DEFAULT 0` (운영 반영 방식 사용자 확인)

### 2-5. 댓글 수정/삭제의 postId 검증 누락

- [x] `updateComment` / `deleteComment`: `findById(commentId)` → `findByIdAndPostId(commentId, postId)` (컨트롤러에서 postId 전달)

### 2-6 ~ 2-8. 팔로우/좋아요

- [x] `FollowService.follow`: 알림 이벤트를 신규 팔로우일 때만 발행 (중복 알림 제거)
- [x] `FollowService.follow`: 팔로우 대상 존재 검증 추가 → `UserNotFoundException`
- [x] `FollowService.follow`: 동시 요청 멱등 처리 — ※ 계획의 try-catch 대신 MySQL `INSERT IGNORE` 원자 쿼리로 구현 (`@Transactional` 안에서 제약 위반 예외를 catch하면 rollback-only 마킹으로 커밋이 실패하는 함정 회피, 반환값 0/1로 신규 여부 판별)
- [x] `LikeService.addLike`: post 조회를 최상단으로 이동 (존재 검증 + authorId 확보 1회, `updated==0` 체크 + 별도 findById 중복 제거)
- [x] `LikeService.addLike`: 동시 요청 멱등 처리 — `INSERT IGNORE` (위와 동일 방식, 중복 시 카운트 증가·이벤트 발행 생략)
- [x] `LikeService.removeLike` / `FollowService.unFollow`: 삭제 행 수를 반환하는 bulk DELETE로 교체 (동시 취소 시 카운트 중복 감소 방지)
- [x] `./gradlew compileJava compileTestJava` 통과

## 3단계 — 팔로워/팔로잉 목록 DTO + 페이징 (breaking, 승인됨)

- [x] `FollowUserResponseDTO` 신규 (`userId`, `nickname`, `profileImageUrl`, `followedAt` + `static from(...)`)
- [x] `FollowRepository`: `findAllByIdFollowingId` / `findAllByIdFollowerId`를 `Pageable` 버전으로 교체 (createdAt 내림차순 정렬)
- [x] `FollowService.getFollowers` / `getFollowing(userId, page, size)`: 상대 userId 수집 → `userRepository.findAllById` + 프로필 일괄 조회 → DTO 매핑
- [x] `FollowController.getFollowers` / `getFollowing`: `page`/`size` 파라미터 추가 (기본 0/20), `List<Follow>` 엔티티 직접 반환 제거
- [x] `./gradlew compileJava compileTestJava` 통과

## 4단계 — 성능 개선

### 4-1. 피드/유저 게시물 N+1

- [x] `PostRepository.findFeedPostIds` 신규 (`SELECT p.id` 버전, 기존 `findFeedPosts` 대체)
- [x] `getFeedPosts`: id 페이징 → `findAllWithMediaByIdIn` fetch join → 순서 재정렬 패턴으로 교체
- [x] `getUserPosts`: 기존 `findPostIdsByAuthorId` 재사용으로 동일 패턴 적용
- [x] "id 조회 → fetch join → 재정렬" 3중 중복(getMyFeeds/getExplorePosts/getTodayShots)을 `loadPostsWithMedia` private 헬퍼로 통합
- [x] 미디어 소유권 검증+연결 중복(createPost/updatePost)을 `attachMedias` 헬퍼로 통합

### 4-2. 댓글 조회 N+1

- [x] `getComments` / `getReplies`: 작성자 id 수집 → `findAllById` + 프로필 일괄 조회 → Map 기반 매핑 (댓글당 2쿼리 제거)
- [x] 작성자 없는 댓글은 예외 대신 log.warn 후 닉네임 null 처리 (트리 구조 보존을 위해 스킵 대신 유지)

### 4-3. 트랜잭션 정리

- [x] `FollowService` 클래스 레벨 `@Transactional` 제거 → 쓰기 메서드만 `@Transactional`, 조회는 `readOnly = true` (LikeService 조회 메서드도 동일 적용)
- [x] `PostRepository` 카운트 UPDATE 4개의 `@Modifying` 옵션 통일 (`clearAutomatically = true`)
- [x] `./gradlew compileJava compileTestJava` 통과

## 5단계 — 구조/가독성 정리

- [x] `snsapi/service/support/AuthorProfileResolver` 신규 (@Component): 단건 `resolve(User)` + 일괄 `resolveUrlMap(Collection<String>)` — 3중 복제된 `resolveAuthorProfileUrl` 통합
- [x] `getAuthor` 중복 정리 (CommentService의 public `getAuthor` 제거, PostService private 헬퍼만 유지)
- [x] `CommentRepository`: `JaasPasswordCallbackHandler` 오타성 import 삭제
- [x] `CommentRepository`: 미사용 `findByPostIdOrderByIdAsc`, `countByPostId` 삭제 (usage 0건 확인)
- [x] `FollowRepository`: 기본 제공과 중복인 `existsById`/`deleteById` 오버라이드, 미사용 `findByIdFollowerId` 삭제
- [x] `PostLikeRepository`: 중복 `existsById` 오버라이드, 미사용 `deleteByIdCustom`·`countByIdPostId` 삭제
- [x] `PostRepository.findByAuthorIdInOrderByIdDesc` 삭제 (미사용), `findByAuthorIdOrderByCreatedAtDesc`도 4-1로 대체되어 삭제
- [x] `TodayShotResponseDTO`: 잘못 붙은 `org.springframework.stereotype.Service` import 삭제
- [x] `PostListResponseDTO` 삭제 (usage 0건 확인)
- [x] `LikeService.removeLike`: 미사용 지역변수 `updated` 제거
- [x] `PostController.deletePost`: `void` → `ResponseEntity<Void>` (204 No Content)
- [x] `Map.of(...)` 즉석 응답 정리: checkLiked → `LikeStatusResponse`, isFollowing → `FollowStatusResponse`, getLikeCount → `LikeCountResponse` 신규 (기존 JSON 필드명 유지, liked/following에 count 필드 추가됨)
- [x] `./gradlew compileJava compileTestJava` 통과

## 6단계 — 테스트 (버그 수정 부분만, PaymentServiceTest 패턴)

### `service/CommentServiceTest` (`@ExtendWith(MockitoExtension)`) — 10개

- [x] 대댓글 있는 부모 삭제 → soft delete + 행 유지 + 카운트 1 감소
- [x] 대댓글 없는 댓글 삭제 → hard delete
- [x] 이미 삭제된 댓글 재삭제 → `CommentNotFoundException`
- [x] 삭제된 댓글에 답글/수정 → `InvalidCommentException`
- [x] 대댓글에 답글 → `InvalidCommentException`
- [x] postId 불일치 삭제 → `CommentNotFoundException`
- [x] 타인 댓글 수정/삭제 → `SnsAccessDeniedException`
- [x] 목록 조회 시 soft delete된 부모는 `"삭제된 댓글입니다."` 대체 문구 + 대댓글 유지

### `service/FollowServiceTest` — 4개

- [x] 신규 팔로우 → 이벤트 1회 발행
- [x] 중복 팔로우 → 이벤트 발행 없음 (`should(never())`)
- [x] 자기 자신 팔로우 → 차단
- [x] 미존재 대상 팔로우 → `UserNotFoundException`

### `service/LikeServiceTest` — 5개

- [x] 신규 좋아요 → 카운트 증가 + 이벤트 발행
- [x] 중복 좋아요 → 카운트 미증가 + 이벤트 미발행 (멱등)
- [x] 미존재 게시물 → `PostNotFoundException`
- [x] 좋아요 취소 → 삭제 성공 시에만 카운트 감소
- [x] 좋아요 상태 아닌 취소 → 카운트 미감소 (멱등)

### `service/PostServiceTest` — 1개

- [x] getTodayShots: 작성자 null 게시물 스킵 (NPE 없음)

### `repository/PostRepositoryTest` (Testcontainers MySQL, `@DataJpaTest`) — 1개

- [x] `findTodayShotIds`: 실제 MySQL 8.0에서 오류 없이 실행 + 미디어 있는 게시물만 likeCount 내림차순 반환 (2-1 회귀 테스트)

### 실행

- [x] `./gradlew test --tests 'com.example.HonBam.snsapi.*'` 통과 — 21개 테스트 전부 성공
- [x] `./gradlew test --tests 'com.example.HonBam.paymentsapi.*'` 통과 — 기존 테스트 회귀 없음
- [x] (부수 수정) Testcontainers 1.19.7 → 1.21.3 업그레이드 + 테스트 JVM에 `api.version=1.41` 지정 — docker-java 기본 API 1.32가 최신 Docker 데몬(최소 1.40 요구)에서 거부되어 Testcontainers 전체가 실패하던 문제 해결 (기존 chat 테스트의 Docker 실패도 같은 원인)

## 머지 전 확인 (리스크)

- [ ] `sns_comment.deleted` 컬럼 운영 DB 반영 방식 확정 (ddl-auto 확인 or 수동 `ALTER TABLE sns_comment ADD COLUMN deleted BIT(1) NOT NULL DEFAULT 0`)
- [ ] `INSERT IGNORE` 쿼리는 MySQL 전용 — 운영 DB가 MySQL 계열인지 확인 (테스트 환경은 MySQL 8.0)
- [ ] 프론트엔드 공지 — 좋아요 취소 응답 `liked`: `true` → `false` (버그 수정이지만 클라이언트가 오동작에 의존했을 수 있음)
- [ ] 프론트엔드 공지 — 팔로워/팔로잉 목록 응답 구조 변경 (Follow 엔티티 → `FollowUserResponseDTO`) + `page`/`size` 파라미터 (기본 20건)
- [ ] 프론트엔드 공지 — 권한 오류 상태코드 변경: 400/401 plain text → 403 `ErrorResponse{errorCode,message}` JSON
- [ ] 프론트엔드 공지 — 게시글 삭제 응답 200 빈 응답 → 204 No Content
- [ ] 프론트엔드 공지 — 삭제된 부모 댓글이 목록에서 사라지는 대신 `deleted: true` + `"삭제된 댓글입니다."`로 표시됨
- [ ] 프론트엔드 공지 — 좋아요 여부/팔로우 여부 조회 응답에 `likeCount`/`followerCount` 필드 추가 (기존 필드는 유지)

## 후속 과제 (이번 범위 밖 — PR 설명에 기록)

- 게시물 삭제 시 댓글/좋아요/Media(S3) 고아 데이터 정리 (이번에 현행 유지로 확정)
- 팔로우 목록 무한스크롤 커서 페이징
- likeCount/commentCount 비정규화 값 정합성 배치 보정
- 대댓글이 모두 삭제된 soft-deleted 부모 댓글의 자동 정리
