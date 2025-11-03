package com.example.HonBam.snsapi.service;

import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.request.PostUpdateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostLikeId;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.PostUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;
    private final PostUtils postUtils;

    // 내 게시물 조회
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getMyFeeds(String userId, int page, int size) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(post -> {
                    boolean liked = isPostLikedByUser(userId, post.getId());
                    return PostResponseDTO.from(post, liked);
                })
                .collect(Collectors.toList());

    }

    @Transactional
    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        // 팔로잉 유저들의 게시물 조회
        List<Post> posts = postRepository.findFeedPosts(userId, PageRequest.of(page, size));
        return posts.stream()
                .map(post -> {
                    boolean liked = isPostLikedByUser(userId, post.getId());
                    return PostResponseDTO.from(post, liked);
                })
                .collect(Collectors.toList());
    }


    // 댓
    @Transactional
    public PostResponseDTO createPost(String userId, PostCreateRequestDTO requestDTO) {

        String jsonImages = postUtils.safeConvertImageUrlsToJson(requestDTO.getImageUrls());

        Post post = Post.builder()
                .authorId(userId)
                .content(requestDTO.getContent())
                .imageUrlsJson(jsonImages)
                .likeCount(0)
                .commentCount(0)
                .build();

        Post saved = postRepository.save(post);
        return PostResponseDTO.from(saved, false);
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

        return PostResponseDTO.from(post, liked);
    }


    // 특정 유저 게시물
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getUserPosts(String userId, String authorId, int page, int size) {

        userRepository.findById(authorId).orElseThrow(
                () -> new UserNotFoundException("사용자가 존재하지 않습니다.")
        );

        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size))
                .stream()
                .map(post -> {
                    boolean liked = isPostLikedByUser(userId, post.getId());
                    return PostResponseDTO.from(post, liked);
                })
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
                .map(post -> {
                    boolean liked = false;
                    if (userId != null) {
                        liked = isPostLikedByUser(userId, post.getId());
                    }
                    return PostResponseDTO.from(post, liked);
                })
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
}

