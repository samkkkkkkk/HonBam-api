package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.CustomUnauthorizedException;
import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.MediaRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.dto.response.TodayShotResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.entity.SnsMedia;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.PostUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final PostUtils postUtils;
    private final ObjectMapper objectMapper;

    // 작성자 추출 메서드
    private User getAuthor(String authorId) {
        return userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));
    }

    // 내 게시물 조회
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getMyFeeds(String userId, int page, int size) {
        List<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return convertToDTOList(posts, userId);
    }

    // 탐색 탭
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getExplorePosts(String userId, String sort, int page, int size) {
        String sortKey = (sort == null || sort.isBlank()) ? "recent" : sort;
        List<Post> posts;
        if ("recent".equalsIgnoreCase(sortKey)) {
            posts = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        } else {
            posts = postRepository.findAllByOrderByLikeCountDesc(PageRequest.of(page, size));
        }

        return convertToDTOList(posts, userId);
    }


    @Transactional(readOnly = true)
    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        List<Post> posts = postRepository.findFeedPosts(userId, PageRequest.of(page, size));
        return convertToDTOList(posts, userId);
    }


    // 게시물 등록
    @Transactional
    public PostResponseDTO createPost(String userId, PostCreateRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));


        Post post = Post.builder()
                .authorId(userId)
                .content(requestDTO.getContent())
                .likeCount(0)
                .commentCount(0)
                .build();

        // Media 저장
        List<SnsMedia> mediaList = new ArrayList<>();
        for (MediaRequestDTO m : requestDTO.getMediaList()) {
            SnsMedia media = SnsMedia.builder()
                    .post(post)
                    .fileKey(m.getFileKey())
                    .fileUrl(m.getFileUrl())
                    .contentType(m.getContentType())
                    .fileSize(m.getFileSize())
                    .sortOrder(m.getSortOrder())
                    .build();
            mediaList.add(media);
        }

        post.getMediaList().addAll(mediaList);

        Post saved = postRepository.save(post);
        return convertToDTO(saved, userId, user, false);
    }

    // 게시물 상세 조회
    @Transactional(readOnly = true)
    public PostResponseDTO getPostDetail(String viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        User author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

        boolean liked = isPostLikedByUser(viewerId, post.getId());

        return convertToDTO(post, viewerId, author, liked);
    }

    // 게시글 수정
    @Transactional
    public PostResponseDTO updatePost(String userId, Long postId, PostUpdateRequestDTO requestDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new CustomUnauthorizedException("본인의 게시글만 수정할 수 있습니다.");
        }
        post.updateContent(requestDTO.getContent());

        List<SnsMedia> oldList = post.getMediaList();

        List<MediaRequestDTO> newList = requestDTO.getMediaList();

        // 기존 리스트와 newList 비교하여 삭제 처리
        // newList에 없는 fileKey는 제거
        List<String> newFileKeys = newList.stream()
                .map(MediaRequestDTO::getFileKey)
                .collect(Collectors.toList());

        List<SnsMedia> toRemove = oldList.stream()
                .filter(media -> !newFileKeys.contains(media.getFileKey()))
                .collect(Collectors.toList());

        toRemove.forEach(post::removeMedia);

        // 5) 추가 처리 + sortOrder 업데이트
        for (MediaRequestDTO dto : newList) {
            SnsMedia existing = oldList.stream()
                    .filter(m -> m.getFileKey().equals(dto.getFileKey()))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                // 새 미디어 추가
                SnsMedia newMedia = SnsMedia.builder()
                        .post(post)
                        .fileKey(dto.getFileKey())
                        .fileUrl(dto.getFileUrl())
                        .contentType(dto.getContentType())
                        .fileSize(dto.getFileSize())
                        .sortOrder(dto.getSortOrder())
                        .build();

                post.addMedia(newMedia);
            } else {
                // 기존 미디어 정렬 순서만 도메인 메서드로 수정
                existing.changeSortOrder(dto.getSortOrder());
            }
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

        boolean liked = isPostLikedByUser(userId, postId);

        return convertToDTO(post, userId, author, liked);
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
                    return convertToDTO(post, viewerId, author, liked);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


    }

    private TodayShotResponseDTO buildTodayShotDTO(Post post, User author) {

        List<SnsMedia> mediaList = post.getMediaList();

        // 이미지가 없으면 null 반환
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        if (author == null) {
            log.warn("오늘의 인증샷 작성자를 찾을 수 없습니다. postId: {}", post.getId());
            return null;
        }

        String authorProfileUrl = postUtils.buildProfileUrl(author);

        List<String> imageUrls = mediaList.stream()
                .sorted(Comparator.comparingInt(SnsMedia::getSortOrder))
                .map(SnsMedia::getFileUrl)
                .collect(Collectors.toList());

        return TodayShotResponseDTO.builder()
                .postId(post.getId())
                .firstImageUrl(imageUrls.get(0))
                .imageUrls(imageUrls)
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .authorNickname(author.getNickname())
                .authorProfileUrl(authorProfileUrl)
                .build();

    }

    // nickname과 profileUrl을 포함하여 DTO로 변환
    private PostResponseDTO convertToDTO(Post post, String viewerId, User author, boolean liked) {
        String nickname = author.getNickname();
        String profileUrl = postUtils.buildProfileUrl(author);
        return PostResponseDTO.from(post, liked, nickname, profileUrl);
    }


    // 오늘의 인증샷 조회
    @Transactional(readOnly = true)
    public List<TodayShotResponseDTO> getTodayShots(int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(0, limit);

        List<Post> posts = postRepository.findTodayShotsOrderByLikes(start, end, pageable);

        // 작성자 정보 일괄 조회
        Set<String> authorIds = posts.stream()
                .map(Post::getAuthorId)
                .collect(Collectors.toSet());

        Map<String, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return posts.stream()
                .map(post -> buildTodayShotDTO(post, authorMap.get(post.getAuthorId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> extractImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<String> urls = objectMapper.readValue(
                    imageUrlsJson,
                    new TypeReference<List<String>>() {}
            );
            return (urls == null) ? Collections.emptyList() : urls;
        } catch (Exception e) {
            log.warn("imageUrlsJson 파싱 실패: {}", imageUrlsJson, e);
            return Collections.emptyList();
        }
    }

}

