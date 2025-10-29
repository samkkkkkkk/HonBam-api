package com.example.HonBam.snsapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.snsapi.dto.request.PostCreateRequestDTO;
import com.example.HonBam.snsapi.dto.response.PostResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.repository.CommentRepository;
import com.example.HonBam.snsapi.repository.FollowRepository;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponseDTO> getMyFeeds(String userId, int page, int size) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(post -> PostResponseDTO.from(post, false))
                .collect(Collectors.toList());

    }

    public List<PostResponseDTO> getFeedPosts(String userId, int page, int size) {
        // 팔로잉 유저들의 게시물 조회
        List<Post> posts = postRepository.findFeedPosts(userId, PageRequest.of(page, size));

        return posts.stream()
                .map(post -> PostResponseDTO.from(post, false))
                .collect(Collectors.toList());
    }

    public void createPost(TokenUserInfo userInfo, PostCreateRequestDTO requestDTO) {

    }
}

