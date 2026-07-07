package com.example.HonBam.snsapi.service;

import com.example.HonBam.snsapi.dto.response.TodayShotResponseDTO;
import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostMedia;
import com.example.HonBam.snsapi.repository.PostLikeRepository;
import com.example.HonBam.snsapi.repository.PostRepository;
import com.example.HonBam.snsapi.service.support.AuthorProfileResolver;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.upload.service.PresignedUrlService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock MediaRepository mediaRepository;
    @Mock PresignedUrlService presignedUrlService;
    @Mock AuthorProfileResolver authorProfileResolver;

    @InjectMocks PostService postService;

    private Post postWithMedia(Long id, String authorId, String fileKey) {
        Post post = Post.builder()
                .id(id)
                .authorId(authorId)
                .content("내용")
                .likeCount(0)
                .commentCount(0)
                .build();
        post.addPostMedia(PostMedia.builder()
                .post(post)
                .media(Media.builder().fileKey(fileKey).build())
                .sortOrder(0)
                .build());
        return post;
    }

    @Test
    @DisplayName("getTodayShots: 작성자가 탈퇴한 게시물은 NPE 없이 스킵된다")
    void todayShotsSkipsPostsWithMissingAuthor() {
        Post withAuthor = postWithMedia(1L, "author-1", "key-1");
        Post withoutAuthor = postWithMedia(2L, "ghost", "key-2");

        given(postRepository.findTodayShotIds(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(1L, 2L)));
        given(postRepository.findAllWithMediaByIdIn(anyList()))
                .willReturn(List.of(withAuthor, withoutAuthor));
        given(userRepository.findAllById(any()))
                .willReturn(List.of(User.builder().id("author-1").nickname("작성자1").build()));
        given(authorProfileResolver.resolveUrlMap(anyCollection())).willReturn(Map.of());
        given(presignedUrlService.generatePresignedGetUrl(anyString())).willReturn("https://presigned/key-1");

        assertThatCode(() -> {
            List<TodayShotResponseDTO> shots = postService.getTodayShots(10);

            assertThat(shots).hasSize(1);
            assertThat(shots.get(0).getPostId()).isEqualTo(1L);
            assertThat(shots.get(0).getAuthorNickname()).isEqualTo("작성자1");
        }).doesNotThrowAnyException();
    }
}
