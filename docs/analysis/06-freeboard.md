# 06. 자유게시판(Freeboard) 도메인 분석

## 1. 개요

자유게시판 도메인은 사용자가 작성하는 **게시글(Freeboard)** 과 그에 달리는 **댓글(FreeboardComment)** 의 CRUD 기능을 제공한다.

- 게시글: 작성 / 목록 조회 / 상세 조회 / 수정 / 삭제
- 댓글: 작성 / 목록 조회 / 수정 / 삭제
- 게시글 1개에 댓글 N개가 달리는 1:N 구조이며, 게시글과 댓글 모두 작성자를 `User` 엔티티와 **`@ManyToOne` 연관관계로 직접 매핑**한다(SNS 도메인이 사용자 식별자를 문자열 컬럼으로 들고 있는 것과 대비됨 — 6장 발견사항 참조).
- 작성/수정/삭제 시 토큰(`@AuthenticationPrincipal TokenUserInfo`)으로부터 사용자를 식별하고, **소유자(작성자) 검증**을 거친 뒤에만 변경을 허용한다.

---

## 2. 구성 요소

| 구성 요소 | 파일 경로 | 역할 |
|-----------|-----------|------|
| `Freeboard` (엔티티) | `src/main/java/com/example/HonBam/freeboardapi/entity/Freeboard.java` | `hb_frboard` 테이블 매핑. 제목/내용/작성자명/작성·수정시각 보유. `User`를 `@ManyToOne(LAZY)`로, `FreeboardComment`를 `@OneToMany(cascade=ALL, orphanRemoval=true)`로 보유 |
| `FreeboardComment` (엔티티) | `src/main/java/com/example/HonBam/freeboardapi/entity/FreeboardComment.java` | `fr_comment` 테이블 매핑. 댓글 내용/작성자(writer)/작성·수정시각 보유. `User`와 `Freeboard`를 각각 `@ManyToOne(LAZY)`로 보유 |
| `FreeboardRequestDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/request/FreeboardRequestDTO.java` | 게시글 작성/수정 요청 DTO. `toEntity(User)` 및 `toEntity(Freeboard, User)` 변환 메서드 보유 |
| `FreeboardCommentRequestDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/request/FreeboardCommentRequestDTO.java` | 댓글 작성 요청 DTO(`comment`, 대상 게시글 `id`). `toEntity(User, Freeboard)` 보유 |
| `CommentModifyRequestDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/request/CommentModifyRequestDTO.java` | 댓글 수정 요청 DTO(`id`, `comment`) |
| `FreeboardResponseDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/response/FreeboardResponseDTO.java` | 게시글 목록 응답 DTO(`posts` 리스트 + `count`) |
| `FreeboardDetailResponseDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/response/FreeboardDetailResponseDTO.java` | 게시글 상세/목록 항목 DTO. `Freeboard` 엔티티에서 제목/내용/작성자명/이메일/표시일자 변환 |
| `FreeboardCommentResponseDTO` | `src/main/java/com/example/HonBam/freeboardapi/dto/response/FreeboardCommentResponseDTO.java` | 댓글 응답 DTO(`commentId`, `comment`, `nickname`, `createTime`, `updateTime`) |
| `FreeboardRepository` | `src/main/java/com/example/HonBam/freeboardapi/repository/FreeboardRepository.java` | 게시글 JPA 리포지토리. 소유자 검증(`isPostOwner`)·소유자 한정 삭제(`deleteByPostIdAndOwner`) JPQL 제공 |
| `FreeboardCommentRepository` | `src/main/java/com/example/HonBam/freeboardapi/repository/FreeboardCommentRepository.java` | 댓글 JPA 리포지토리. 작성자 닉네임 조인 조회·소유자 검증·소유자 한정 삭제 JPQL 제공 |
| `FreeboardService` | `src/main/java/com/example/HonBam/freeboardapi/Service/FreeboardService.java` | 게시글/댓글 비즈니스 로직. 폴더명은 대문자 `Service`이나 `package` 선언은 소문자 `service`(발견사항 ① 참조). `@Transactional` 클래스 부여 |
| `FreeboardController` | `src/main/java/com/example/HonBam/freeboardapi/api/FreeboardController.java` | REST 진입점. `/api/freeboard` 게시글 CRUD 및 `/api/freeboard/comment` 댓글 CRUD 제공 |

---

## 3. API 엔드포인트

베이스 경로: `/api/freeboard` (`@RequestMapping("/api/freeboard")`, `@CrossOrigin`)

### 게시글

| 메서드 | 경로 | 인증(`TokenUserInfo`) | 요청 | 요약 |
|--------|------|------------------------|------|------|
| `POST` | `/api/freeboard` | 사용 O (`@AuthenticationPrincipal`) | `@RequestBody FreeboardRequestDTO` | 게시글 작성 후 전체 목록(`FreeboardResponseDTO`) 반환 (`FreeboardController.java:27-33`) |
| `GET` | `/api/freeboard` | 사용 X | 없음 | 전체 게시글 목록 조회 (`FreeboardController.java:36-39`) |
| `GET` | `/api/freeboard/{id}` | 사용 X | `@PathVariable id` | 게시글 상세 조회(`FreeboardDetailResponseDTO`) (`FreeboardController.java:62-65`) |
| `PATCH` | `/api/freeboard/{id}` | 사용 O | `@PathVariable id` + `FreeboardRequestDTO` | 소유자 검증 후 게시글 수정 (`FreeboardController.java:51-59`) |
| `DELETE` | `/api/freeboard/{id}` | 사용 O | `@PathVariable id` | `userInfo.getUserId()`를 넘겨 소유자 한정 삭제, 목록 반환 (`FreeboardController.java:42-48`) |

### 댓글

| 메서드 | 경로 | 인증(`TokenUserInfo`) | 요청 | 요약 |
|--------|------|------------------------|------|------|
| `POST` | `/api/freeboard/comment` | 사용 O | `@RequestBody FreeboardCommentRequestDTO` | 댓글 등록 후 해당 게시글 댓글 목록 반환 (`FreeboardController.java:68-75`) |
| `GET` | `/api/freeboard/comment` | 사용 X | `@RequestParam Long id` (게시글 id) | 게시글의 댓글 목록 조회 (`FreeboardController.java:78-85`) |
| `PATCH` | `/api/freeboard/comment` | 사용 O | `@RequestBody CommentModifyRequestDTO` | 소유자 검증 후 댓글 수정, 목록 반환 (`FreeboardController.java:99-105`) |
| `DELETE` | `/api/freeboard/comment/{commentId}` | 사용 O | `@PathVariable commentId` | 소유자 한정 댓글 삭제, 목록 반환 (`FreeboardController.java:88-96`) |

> 조회 계열(게시글 목록/상세, 댓글 목록)에는 `@AuthenticationPrincipal TokenUserInfo` 파라미터가 없어 토큰 없이 호출 가능하며, 변경 계열(작성/수정/삭제)에만 `TokenUserInfo`가 주입된다. 단, 컨트롤러 자체에는 인증 강제 로직이 없으므로 실제 차단 여부는 `WebSecurityConfig`의 URL 매칭에 의존한다.

---

## 4. 데이터 흐름

### 게시글 작성 (`createContent`, `FreeboardService.java:75-82`)
1. `findUserByToken(userInfo)` — 토큰의 `userId`로 `UserRepository.findById` 조회, 없으면 `RuntimeException`.
2. `requestDto.toEntity(user)` — DTO를 `Freeboard` 엔티티로 변환하며 `user` 연관관계를 채움(`FreeboardRequestDTO.java:21-28`).
3. `freeboardRepository.save(...)` 후 `retrieve()`로 **전체 목록을 재조회하여 반환**.

### 게시글 수정 (`modify`, `FreeboardService.java:113-124`)
1. `findUserByToken` → 사용자 조회.
2. `ensurePostOwner(postId, user.getId())` — `FreeboardRepository.isPostOwner`(JPQL `count(f)>0 ... f.user.id = :userId`)가 false면 `SecurityException`.
3. `getPostOrThrow(postId)`로 기존 엔티티 조회 후 `requestDTO.toEntity(found, user)`로 **id를 유지한 새 엔티티를 빌드**(제목/내용은 DTO 값, `userName`은 기존 값 유지)하고 `updateDate`를 수동 세팅하여 `save`.

### 게시글 삭제 (`delete`, `FreeboardService.java:105-109`)
1. 컨트롤러가 `userInfo.getUserId()` 문자열을 직접 전달.
2. `freeboardRepository.deleteByPostIdAndOwner(postId, userId)` — **소유자 조건이 포함된 `@Modifying` JPQL DELETE**를 실행하고, 영향 행수 `affected == 0`이면 “삭제 권한 없음 또는 대상 없음”으로 `SecurityException`.
3. `retrieve()`로 목록 반환.

### 댓글 작성/수정/삭제
- 작성(`commentRegist`, `:129-138`): `findUserByToken` → `getPostOrThrow(dto.getId())` → `dto.toEntity(user, post)`(작성자 닉네임을 `writer`에 저장) → `save` → 댓글 목록 반환.
- 수정(`modify(CommentModifyRequestDTO ...)`, `:161-172`): `ensureCommentOwner`로 소유자 검증 후 엔티티의 `comment`/`updateTime` 갱신.
- 삭제(`commentDelete`, `:149-157`): 먼저 댓글의 소속 게시글 id를 확보한 뒤 `deleteByCommentIdAndOwner`(소유자 조건 DELETE) 실행, `affected==0`이면 `SecurityException`.

### 작성자 검증 흐름(검증 전용 메서드)
- `validatePostWriter(userInfo, postId)`(`:175-179`)와 `validateCommentWriter(userInfo, commentId)`(`:181-185`)는 토큰 사용자를 조회한 뒤 각각 `isPostOwner` / `isCommentOwner`의 boolean을 그대로 반환하는 **읽기 전용 검증 메서드**다. 단, 현재 `FreeboardController`에는 이 두 메서드를 호출하는 엔드포인트가 없어 **외부에서 직접 노출되지 않는다**(미사용 공개 메서드).
- 실제 변경 경로의 권한 처리는 두 갈래로 나뉜다.
  - **선검증 방식**: 게시글 수정·댓글 수정은 `ensurePostOwner`/`ensureCommentOwner`로 먼저 검증 후 변경(검증 실패 시 `SecurityException`).
  - **조건부 삭제 방식**: 게시글·댓글 삭제는 별도 검증 없이 소유자 조건이 포함된 DELETE의 영향 행수로 권한을 판정.

---

## 5. 영속성 / 연관관계

### User 연관관계 — `@ManyToOne` 직접 매핑
- `Freeboard.user`(`Freeboard.java:47-50`)와 `FreeboardComment.user`(`FreeboardComment.java:40-44`)는 모두 `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "user_id")`로 **`User` 엔티티를 객체 연관관계로 직접 매핑**한다.
- 따라서 소유자 검증 JPQL이 `f.user.id` / `c.user.id`처럼 **연관 객체의 식별자로 조인 조건을 표현**한다(`FreeboardRepository.java:10-15`, `FreeboardCommentRepository.java:16-21`).
- 이는 SNS 도메인이 `Post.authorId`를 `String` 컬럼으로 들고 있는 방식(`snsapi/entity/Post.java:27-28`)과 **연관관계 전략이 상반**된다(발견사항 ② 참조).

### Freeboard ↔ FreeboardComment — `@OneToMany(cascade = ALL)`
- `Freeboard.commentList`(`Freeboard.java:52-56`): `@OneToMany(mappedBy = "freeboard", cascade = CascadeType.ALL, orphanRemoval = true)` + `@Builder.Default ... new ArrayList<>()` + `@JsonIgnore`. 연관관계의 주인은 `FreeboardComment.freeboard`(`FreeboardComment.java:46-50`, `@JoinColumn(name = "post_id")`).
- `cascade = ALL` + `orphanRemoval = true`이므로 영속 컨텍스트를 통한 게시글 삭제/컬렉션 정리 시 댓글이 함께 정리되도록 의도되어 있다. 다만 실제 삭제 경로는 JPQL `@Modifying` 벌크 DELETE를 사용한다(발견사항 ③ 참조).

### 기타 매핑 특징
- 시간 컬럼: `@CreationTimestamp createDate`(updatable=false), `@UpdateTimestamp updateDate`(insertable=false) — 게시글. 댓글도 `createTime`/`updateTime`에 동일 패턴(`FreeboardComment.java:31-38`).
- 식별자: `Freeboard.id`는 원시 타입 `long`(`Freeboard.java:24-26`), `FreeboardComment.commentId`는 래퍼 `Long`(`FreeboardComment.java:22-24`)로 타입·명명이 일관되지 않다.
- `FreeboardDetailResponseDTO` 생성자(`:24-35`)는 `freeboard.getUser().getUserName()`·`getEmail()`을 호출하므로 **`User` 연관(LAZY) 초기화가 필요**하며, 목록 조회 시 N+1 가능성이 있다(영속성 컨텍스트 트랜잭션 안에서 `retrieve()`가 수행됨).

---

## 6. 발견사항

코드 재확인 후 정리. 각 항목은 근거 `파일:라인`과 확신도(CONFIRMED/PLAUSIBLE)를 표기한다.

**① 서비스 폴더명 대문자 `Service` — 패키지 컨벤션 불일치 [CONFIRMED]**
- 디렉터리 경로는 `freeboardapi/Service/FreeboardService.java`(대문자 `Service`)인데, 파일의 패키지 선언은 소문자 `package com.example.HonBam.freeboardapi.service;`이다(`FreeboardService.java:1`). 컨트롤러도 소문자로 임포트한다(`FreeboardController.java:8`).
- Windows 등 대소문자 비구분 파일시스템에서는 컴파일·실행되지만, 다른 도메인(`recipeapi/service`, `userapi/...` 등)은 소문자 폴더를 쓰므로 **이 도메인만 폴더명 케이싱이 어긋난다**. 대소문자 구분 환경(리눅스 빌드)에서 잠재적 빌드 문제 소지가 있다.

**② SNS는 사용자 식별자 문자열 참조, freeboard는 `@ManyToOne` 직접 매핑 — 도메인 간 연관관계 전략 불일치 [CONFIRMED]**
- Freeboard/FreeboardComment는 `User`를 `@ManyToOne` + `@JoinColumn("user_id")`로 매핑(`Freeboard.java:48-50`, `FreeboardComment.java:42-44`).
- 반면 SNS `Post`는 `@Column(name="author_id", length=36) private String authorId;`로 사용자 식별자를 **문자열 스칼라 컬럼**으로만 보유한다(`snsapi/entity/Post.java:27-28`). SNS의 `PostLikeId.userId`도 동일하게 `String`이다(`snsapi/entity/PostLikeId.java:17`).
- 동일 프로젝트 내에서 사용자 참조 모델링이 두 갈래(객체 연관 vs ID 문자열)로 갈려 **일관성·조인 전략이 도메인마다 다르다**.

**③ 작성자(소유자) 검증·권한 처리 관찰 [CONFIRMED, 일부 PLAUSIBLE]**
- 검증 경로가 **세 가지로 혼재**한다: (a) `ensurePostOwner`/`ensureCommentOwner` 선검증 후 변경(수정), (b) 소유자 조건 포함 벌크 DELETE의 영향 행수 판정(삭제), (c) boolean을 반환하는 `validatePostWriter`/`validateCommentWriter`. [CONFIRMED: `FreeboardService.java:60-70, 105-109, 153-157, 175-185`]
- `validatePostWriter`/`validateCommentWriter`는 정의되어 있으나 **컨트롤러에서 호출되지 않는 미사용 공개 메서드**다(`FreeboardController.java` 전체에 호출 없음). [CONFIRMED]
- 검증 실패 예외가 `SecurityException`/`RuntimeException`/`IllegalArgumentException`으로 제각각이며, 도메인 내 전역 예외 핸들러가 없어 클라이언트에 어떤 HTTP 상태로 매핑되는지는 프로젝트 공통 핸들러 설정에 의존한다. [PLAUSIBLE — 본 도메인 코드만으로는 상태코드 미확정]

**④ 게시글 벌크 DELETE가 `cascade ALL`/`orphanRemoval`을 우회 [PLAUSIBLE]**
- 게시글 삭제는 `@Modifying @Query("delete from Freeboard f where f.id=:postId and f.user.id=:userId")`(`FreeboardRepository.java:13-15`)로 수행된다. JPQL 벌크 DELETE는 **영속성 컨텍스트의 cascade/orphanRemoval을 적용하지 않으므로**, `Freeboard ↔ FreeboardComment`의 `cascade=ALL, orphanRemoval=true`(`Freeboard.java:53`)에 의존하는 자식 댓글 자동 삭제가 동작하지 않는다. DB FK 제약이 `ON DELETE CASCADE`가 아니라면 자식 댓글이 남아 있는 게시글 삭제 시 무결성 위반 가능성이 있다.

**⑤ 댓글 목록 조회 JPQL의 select 컬럼 순서/타입이 DTO 생성자와 어긋남 [PLAUSIBLE — 잠재적 버그]**
- 조회 JPQL: `select c.id, c.comment, c.createTime, c.writer, u.nickname ...`(`FreeboardCommentRepository.java:13`).
- 매핑 대상 생성자: `FreeboardCommentResponseDTO(commentId, comment, nickname, createTime, updateTime)`(`FreeboardCommentResponseDTO.java:10-16`).
- 위치 기반 매핑 시 3번째 값 `c.createTime`(`LocalDateTime`)이 `nickname`(`String`)에, 4번째 `c.writer`(`String`)가 `createTime`(`LocalDateTime`)에, 5번째 `u.nickname`이 `updateTime`에 들어가게 되어 **순서·타입이 모두 어긋난다**(`updateTime`에 해당하는 컬럼은 select되지 않음). 런타임 변환 오류 또는 잘못된 데이터 매핑이 발생할 소지가 있다.

**⑥ 식별자 명명/JPQL `id` 표현 [PLAUSIBLE]**
- `FreeboardComment`의 식별자 필드는 `commentId`(`FreeboardComment.java:24`)인데, 리포지토리 JPQL은 `c.id`로 참조한다(`FreeboardCommentRepository.java:13,16,20`). Hibernate가 식별자 의사속성 `id`로 해석해 동작할 수 있으나, 필드명(`commentId`)과 표현(`c.id`)이 달라 **가독성·이식성 측면에서 혼선** 소지가 있다.
