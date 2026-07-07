package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.SnsAccessDeniedException;
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
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AuthorProfileResolver authorProfileResolver;

    // 작성자 추출 메서드
    private User getAuthor(String authorId) {
        return userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));
    }

    // 내 게시물 조회
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getMyFeeds(String userId, int page, int size) {
        List<Long> postIds = postRepository
                .findPostIdsByAuthorId(userId, PageRequest.of(page, size))
                .getContent();
        return convertToDTOList(loadPostsWithMedia(postIds), userId);
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
        return convertToDTOList(loadPostsWithMedia(postIds), userId);
    }

    // 팔로잉 피드
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        List<Long> postIds = postRepository
                .findFeedPostIds(userId, PageRequest.of(page, size))
                .getContent();
        return convertToDTOList(loadPostsWithMedia(postIds), userId);
    }

    // 특정 유저 게시물
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getUserPosts(String userId, String authorId, int page, int size) {
        List<Long> postIds = postRepository
                .findPostIdsByAuthorId(authorId, PageRequest.of(page, size))
                .getContent();
        return convertToDTOList(loadPostsWithMedia(postIds), userId);
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

        attachMedias(post, requestDTO.getMediaIds(), userId);

        User author = getAuthor(userId);

        Post saved = postRepository.save(post);

        return PostResponseDTO.from(
                saved,
                false,
                author.getNickname(),
                authorProfileResolver.resolve(author),
                buildPostMediaResponses(saved)
        );
    }

    // 게시물 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDTO getPostDetail(String viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        User author = getAuthor(post.getAuthorId());

        boolean liked = isPostLikedByUser(viewerId, post.getId());

        return convertToDTO(post, author, liked, authorProfileResolver.resolve(author));
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
            throw new SnsAccessDeniedException("본인의 게시글만 수정할 수 있습니다.");
        }

        post.updateContent(requestDTO.getContent());
        post.clearPostMedias();

        attachMedias(post, requestDTO.getMediaIds(), userId);

        User author = getAuthor(post.getAuthorId());
        boolean liked = isPostLikedByUser(userId, postId);

        return PostResponseDTO.from(
                post,
                liked,
                author.getNickname(),
                authorProfileResolver.resolve(author),
                buildPostMediaResponses(post)
        );
    }

    // 게시물 삭제
    @Transactional
    public void deletePost(String userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시물을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new SnsAccessDeniedException("본인의 게시글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    // 오늘의 인증샷 조회
    @Transactional(readOnly = true)
    public List<TodayShotResponseDTO> getTodayShots(int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Long> postIds = postRepository
                .findTodayShotIds(start, end, PageRequest.of(0, limit))
                .getContent();

        List<Post> posts = loadPostsWithMedia(postIds);
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Map<String, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<String, String> profileUrlMap = authorProfileResolver.resolveUrlMap(authorIds);

        return posts.stream()
                .map(post -> {
                    User author = authorMap.get(post.getAuthorId());
                    if (author == null) {
                        log.warn("오늘의 인증샷 작성자를 찾을 수 없습니다. postId: {}, authorId: {}",
                                post.getId(), post.getAuthorId());
                        return null;
                    }
                    return buildTodayShotDTO(post, author, profileUrlMap.get(author.getId()));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 미디어 소유권 검증 후 게시물에 연결
    private void attachMedias(Post post, List<Long> mediaIds, String userId) {
        if (mediaIds == null) {
            return;
        }
        int order = 0;
        for (Long mediaId : mediaIds) {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

            if (!media.getUploaderId().equals(userId)) {
                throw new SnsAccessDeniedException("본인의 미디어만 사용할 수 있습니다.");
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

    // id 리스트로 미디어까지 fetch join 후 id 순서대로 재정렬
    private List<Post> loadPostsWithMedia(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Post> postMap = postRepository.findAllWithMediaByIdIn(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        return postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

        // 작성자들의 프로필 URL 일괄 조회
        Map<String, String> profileUrlMap = authorProfileResolver.resolveUrlMap(authorIds);

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
                    return convertToDTO(post, author, liked, profileUrlMap.get(author.getId()));
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TodayShotResponseDTO buildTodayShotDTO(Post post, User author, String profileUrl) {

        List<PostMedia> postMedias = post.getPostMedias();
        if (postMedias == null || postMedias.isEmpty()) {
            return null;
        }

        List<String> imageUrls = postMedias.stream()
                .sorted(Comparator.comparingInt(PostMedia::getSortOrder))
                .map(pm -> presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey()))
                .collect(Collectors.toList());

        return TodayShotResponseDTO.builder()
                .postId(post.getId())
                .firstImageUrl(imageUrls.get(0))
                .imageUrls(imageUrls)
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .authorNickname(author.getNickname())
                .authorProfileUrl(profileUrl)
                .build();
    }

    // nickname과 profileUrl을 포함하여 DTO로 변환
    private PostResponseDTO convertToDTO(Post post, User author, boolean liked, String profileUrl) {
        return PostResponseDTO.from(
                post,
                liked,
                author.getNickname(),
                profileUrl,
                buildPostMediaResponses(post)
        );
    }

    private List<PostMediaResponseDTO> buildPostMediaResponses(Post post) {
        return post.getPostMedias().stream()
                .map(pm -> {
                    String mediaUrl = presignedUrlService.generatePresignedGetUrl(pm.getMedia().getFileKey());
                    return PostMediaResponseDTO.from(pm, mediaUrl);
                })
                .collect(Collectors.toList());
    }

}
