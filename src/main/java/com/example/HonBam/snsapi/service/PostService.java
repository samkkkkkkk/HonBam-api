package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.PostNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.dto.response.TodayShotResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.userapi.entity.LoginProvider;
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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
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
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
    }

    // 탐색 탭
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getExplorePosts(String userId, String sort, int page, int size) {

        List<Post> posts;
        if ("recent".equalsIgnoreCase(sort)) {
            posts = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        } else {
            posts = postRepository.findAllByOrderByLikeCountDesc(PageRequest.of(page, size));
        }

        return posts.stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
    }


    @Transactional
    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        return postRepository.findFeedPosts(userId, PageRequest.of(page, size))
                .stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
    }


    // 게시물 등록
    @Transactional
    public PostResponseDTO createPost(String userId, PostCreateRequestDTO requestDTO) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));


        String jsonImages = postUtils.safeConvertImageUrlsToJson(requestDTO.getImageUrls());

        Post post = Post.builder()
                .authorId(userId)
                .content(requestDTO.getContent())
                .imageUrlsJson(jsonImages)
                .likeCount(0)
                .commentCount(0)
                .build();

        Post saved = postRepository.save(post);
        return convertToDTO(saved, userId);
    }


    // 게시글 수정
    @Transactional
    public PostResponseDTO updatePost(String userId, Long postId, PostUpdateRequestDTO requestDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new SecurityException("본인의 게시글만 수정할 수 있습니다.");
        }

        boolean liked = isPostLikedByUser(userId, post.getId());
        String jsonImages = postUtils.safeConvertImageUrlsToJson(requestDTO.getImageUrls());
        post.update(requestDTO.getContent(), jsonImages);
        User author = getAuthor(post.getAuthorId());
        return convertToDTO(post, userId);
    }


    // 특정 유저 게시물
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getUserPosts(String userId, String authorId, int page, int size) {

        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size))
                .stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
    }

    
    // 게시물 삭제
    @Transactional
    public void deletePost(String userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시물을 찾을 수 없습니다."));

        if (!post.getAuthorId().equals(userId)) {
            throw new SecurityException("본인의 게시글만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    // 특정 사용자가 특정 게시물에 좋아요를 눌렀는지 확인
    private boolean isPostLikedByUser(String userId, Long postId) {
        return postLikeRepository.existsById(new PostLikeId(userId, postId));
    }

    // 프로필 url 생성
    private String buildProfileUrl(User author) {
        if (author.getLoginProvider() != LoginProvider.LOCAL) {
            return author.getProfileImg();
        }

        if (author.getProfileImg() == null) {
            return "/default-profile.png";
        }

        return "uploads/" + author.getProfileImg();
    }

    // nickname과 profileUrl을 포함하여 DTO로 변환
    private PostResponseDTO convertToDTO(Post post, String viewerId) {
        boolean liked = isPostLikedByUser(viewerId, post.getId());

        User author = userRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

        String nickname = author.getNickname();
        String profileUrl = buildProfileUrl(author);

        return PostResponseDTO.from(post, liked, nickname, profileUrl);
    }

    @Transactional(readOnly = true)
    public List<TodayShotResponseDTO> getTodayShots(int limit) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Pageable pageable = PageRequest.of(0, limit);

        List<Post> posts = postRepository.findTodayShotsOrderByLikes(start, end, pageable);

        return posts.stream()
                .map(post -> {
                    List<String> imageUrls = extractImageUrls(post.getImageUrlsJson());
                    if (imageUrls.isEmpty()) return null;

                    User author = userRepository.findById(post.getAuthorId())
                            .orElseThrow(() -> new UserNotFoundException("작성자를 찾을 수 없습니다."));

                    String authorProfileUrl = buildProfileUrl(author);

                    return TodayShotResponseDTO.builder()
                            .postId(post.getId())
                            .firstImageUrl(imageUrls.get(0))
                            .imageUrls(imageUrls)
                            .content(post.getContent())
                            .likeCount(post.getLikeCount())
                            .authorNickname(author.getNickname())
                            .authorProfileUrl(authorProfileUrl)
                            .build();
                })
                // 실제로 이미지가 하나 이상 있는 경우만 오늘의 인증샷으로 사용
                .filter(dto -> dto.getImageUrls() != null && !dto.getImageUrls().isEmpty())
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

    public PostResponseDTO getPostDetail(String viewerId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        return convertToDTO(post, viewerId);
    }
}

