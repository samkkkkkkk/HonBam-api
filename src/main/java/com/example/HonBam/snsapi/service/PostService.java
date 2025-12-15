package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.CustomUnauthorizedException;
import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostMediaResponseDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.dto.response.TodayShotResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.entity.PostMedia;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.entity.UserProfileMedia;
import com.example.HonBam.userapi.repository.UserProfileMediaRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final MediaRepository mediaRepository;
    private final PresignedUrlService presignedUrlService;
    private final UserProfileMediaRepository userProfileMediaRepository;

    // 작성자 추출 메서드
    private User getAuthor(String authorId) {
        return userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));
    }

    // 내 게시물 조회
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getMyFeeds(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 페이징 -> Id 먼저 조회
        Page<Long> postIdsPage = postRepository.findPostIdsByAuthorId(userId, pageable);
        List<Long> postIds = postIdsPage.getContent();

        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        // id 리스트로 Fetch join
        List<Post> posts = postRepository.findAllWithMediaByIdIn(postIds);

        // id 리스트 순서대로 재정렬
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> sortedPosts = postIds.stream()
                .map(postMap::get)
                .collect(Collectors.toList());

        return convertToDTOList(sortedPosts, userId);
    }

    // 탐색 탭
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getExplorePosts(String userId, String sort, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Long> postIds;
        if ("recent".equalsIgnoreCase(sort)) {
            postIds = postRepository.findAllPostIdsOrderByCreatedAtDesc(pageable).getContent();
        } else {
            postIds = postRepository.findAllPostIdsOrderByLikeCountDesc(pageable).getContent();
        }

        if (postIds.isEmpty()) return Collections.emptyList();

        List<Post> posts = postRepository.findAllWithMediaByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        List<Post> sortedPosts = postIds.stream().map(postMap::get).collect(Collectors.toList());

        return convertToDTOList(sortedPosts, userId);
    }


    @Transactional(readOnly = true)
    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        List<Post> posts = postRepository.findFeedPosts(userId, PageRequest.of(page, size));
        return convertToDTOList(posts, userId);
    }


    // 게시물 등록
    @Transactional
    public PostResponseDTO createPost(String userId, PostCreateRequestDTO requestDTO) {

        Post post = Post.builder()
                .authorId(userId)
                .content(requestDTO.getContent())
                .likeCount(0)
                .commentCount(0)
                .build();

        if (requestDTO.getMediaIds() != null) {
            int order = 0;
            for (Long mediaId : requestDTO.getMediaIds()) {

                Media media = mediaRepository.findById(mediaId)
                        .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

                // 소유자 검증
                if (!media.getUploaderId().equals(userId)) {
                    throw new CustomUnauthorizedException("본인의 미디어만 사용할 수 있습니다.");
                }

                post.addPostMedia(
                        PostMedia.builder()
                                .post(post)
                                .media(media)
                                .sortOrder(order++)
                                .build()
                );
            }
        }

        User author = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

        Post saved = postRepository.save(post);

        String authorProfileUrl = resolveAuthorProfileUrl(author);

        List<PostMediaResponseDTO> mediaResponseList = buildPostMediaResponses(saved);

        return PostResponseDTO.from(
                saved,
                false,
                author.getNickname(),
                authorProfileUrl,
                mediaResponseList
        );
    }

    // 게시물 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDTO getPostDetail(String viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        User author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

        boolean liked = isPostLikedByUser(viewerId, post.getId());

        String authorProfileUrl = resolveAuthorProfileUrl(author);

        return convertToDTO(post, author, liked, authorProfileUrl);
    }

    // 게시글 수정
    @Transactional
    public PostResponseDTO updatePost(
            String userId,
            Long postId,
            PostUpdateRequestDTO requestDTO
    ) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new CustomUnauthorizedException("본인의 게시글만 수정할 수 있습니다.");
        }

        post.updateContent(requestDTO.getContent());
        post.clearPostMedias();

        if (requestDTO.getMediaIds() != null) {
            int order = 0;
            for (Long mediaId : requestDTO.getMediaIds()) {
                Media media = mediaRepository.findById(mediaId)
                        .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

                post.addPostMedia(
                        PostMedia.builder()
                                .post(post)
                                .media(media)
                                .sortOrder(order++)
                                .build()
                );
            }
        }

        User author = getAuthor(post.getAuthorId());
        boolean liked = isPostLikedByUser(userId, postId);

        List<PostMediaResponseDTO> mediaResponseList = buildPostMediaResponses(post);
        String authorProfileUrl = resolveAuthorProfileUrl(author);

        return PostResponseDTO.from(
                post,
                liked,
                author.getNickname(),
                authorProfileUrl,
                mediaResponseList
        );
    }


    // 특정 유저 게시물
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getUserPosts(String userId, String authorId, int page, int size) {
        List<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size));
        return convertToDTOList(posts, userId);
    }

    // 게시물 삭제
    @Transactional
    public void deletePost(String userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시물을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new SecurityException("본인의 게시글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    // 특정 사용자가 특정 게시물에 좋아요를 눌렀는지 확인
    private boolean isPostLikedByUser(String userId, Long postId) {
        return postLikeRepository.existsById(new PostLikeId(userId, postId));
    }

    private List<PostResponseDTO> convertToDTOList(List<Post> posts, String viewerId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 작성자 정보 일괄 조회
        Set<String> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Map<String, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 작성자들의 프로필 일괄 조회
        List<UserProfileMedia> profiles = userProfileMediaRepository.findByUser_IdIn(authorIds);

        // 프로필 이미지 Key를 Map으로 변환
        Map<String, String> profileKeyMap = profiles.stream()
                .collect(Collectors.toMap(
                        pm -> pm.getUser().getId(),
                        pm -> pm.getMedia().getFileKey(),
                        (existing, replacement) -> existing
                ));
        // 좋아요 정보 일괄 조회
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toList());

        Set<Long> likedPostIds = postLikeRepository
                .findByUserIdAndPostIdIn(viewerId, postIds).stream()
                .map(PostLikeId::getPostId)
                .collect(Collectors.toSet());


        return posts.stream()
                .map(post -> {
                    User author = authorMap.get(post.getAuthorId());
                    if (author == null) {
                        log.warn("작성자를 찾을 수 없습니다. postId: {}, authorId: {}",
                                post.getId(), post.getAuthorId());
                        return null;
                    }
                    boolean liked = likedPostIds.contains(post.getId());
                    String profileKey = profileKeyMap.get(author.getId());
                    String profileUrl = (profileKey != null)
                            ? presignedUrlService.generatePresignedGetUrl(profileKey)
                            : null;
                    return convertToDTO(post, author, liked, profileUrl);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


    }

    private TodayShotResponseDTO buildTodayShotDTO(Post post, User author, String profileUrl) {

        List<PostMedia> postMedias = post.getPostMedias();
        if (postMedias == null || postMedias.isEmpty()) {
            return null;
        }
        if (author == null) {
            log.warn("오늘의 인증샷 작성자를 찾을 수 없습니다. postId: {}", post.getId());
            return null;
        }

        List<String> imageUrls = postMedias.stream()
                .sorted(Comparator.comparingInt(PostMedia::getSortOrder))
                .map(pm -> presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey())) // 👈 여기!
                .collect(Collectors.toList());

        String firstImageUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        return TodayShotResponseDTO.builder()
                .postId(post.getId())
                .firstImageUrl(firstImageUrl)
                .imageUrls(imageUrls)
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .authorNickname(author.getNickname())
                .authorProfileUrl(profileUrl)
                .build();
    }

    // nickname과 profileUrl을 포함하여 DTO로 변환
    private PostResponseDTO convertToDTO(Post post, User author, boolean liked, String profileUrl) {

        List<PostMediaResponseDTO> mediaResponseList = post.getPostMedias().stream()
                // .sorted(...) // Post 엔티티에서 @OrderBy를 썼다면 생략 가능, 아니면 정렬 수행
                .map(pm -> {
                    String mediaUrl = presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey());
                    return PostMediaResponseDTO.from(pm, mediaUrl);
                })
                .collect(Collectors.toList());

        return PostResponseDTO.from(
                post,
                liked,
                author.getNickname(),
                profileUrl,
                mediaResponseList
        );
    }

    // 오늘의 인증샷 조회
    @Transactional(readOnly = true)
    public List<TodayShotResponseDTO> getTodayShots(int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(0, limit);

        List<Long> postIds = postRepository.findTodayShotIds(start, end, pageable).getContent();

        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Post> posts = postRepository.findAllWithMediaByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> sortedPosts = postIds.stream()
                .map(postMap::get)
                .collect(Collectors.toList());
        // 작성자 정보 일괄 조회
        Set<String> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Map<String, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<UserProfileMedia> profiles = userProfileMediaRepository.findByUser_IdIn(authorIds);
        Map<String, String> profileKeyMap = profiles.stream()
                .collect(Collectors.toMap(
                        pm -> pm.getUser().getId(),
                        pm -> pm.getMedia().getFileKey(),
                        (existing, replacement) -> existing
                ));

        return sortedPosts.stream()
                .map(post -> {
                    User author = authorMap.get(post.getAuthorId());

                    String profileKey = profileKeyMap.get(author.getId());
                    String profileUrl = (profileKey != null)
                            ? presignedUrlService.generatePresignedGetUrl(profileKey)
                            : null;

                    return buildTodayShotDTO(post, author, profileUrl);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String resolveAuthorProfileUrl(User author) {
        return userProfileMediaRepository.findByUser(author)
                .map(u -> presignedUrlService.generatePresignedGetUrl(u.getMedia().getFileKey()))
                .orElse(null);
    }

    private List<PostMediaResponseDTO> buildPostMediaResponses(Post post) {
        return post.getPostMedias().stream()
                .map(pm -> {
                    String mediaUrl =
                            presignedUrlService.generatePresignedGetUrl(
                                    pm.getMedia().getFileKey()
                            );
                    return PostMediaResponseDTO.from(pm, mediaUrl);
                })
                .collect(Collectors.toList());
    }

}

