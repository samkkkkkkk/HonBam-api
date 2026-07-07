package com.example.HonBam.snsapi.repository;

import com.example.HonBam.snsapi.entity.Post;
import com.example.HonBam.snsapi.entity.PostMedia;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// findTodayShotIds가 실제 MySQL에서 실행 가능한지 검증
// (기존 DISTINCT + SELECT에 없는 컬럼 ORDER BY 조합은 MySQL에서 오류 — 회귀 테스트)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostRepositoryTest {

    @Container
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("honbam_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired PostRepository postRepository;
    @Autowired TestEntityManager em;

    private Post savePost(String authorId, int likeCount, boolean withMedia) {
        Post post = Post.builder()
                .authorId(authorId)
                .content("내용")
                .likeCount(likeCount)
                .commentCount(0)
                .build();
        em.persist(post);

        if (withMedia) {
            Media media = Media.builder()
                    .uploaderId(authorId)
                    .mediaPurpose(MediaPurpose.POST)
                    .fileKey("key-" + authorId + "-" + likeCount)
                    .contentType("image/jpeg")
                    .fileSize(100L)
                    .build();
            em.persist(media);

            PostMedia postMedia = PostMedia.builder()
                    .post(post)
                    .media(media)
                    .sortOrder(0)
                    .build();
            em.persist(postMedia);
        }
        return post;
    }

    @Test
    @DisplayName("findTodayShotIds: MySQL에서 오류 없이 실행되고, 미디어 있는 게시물만 likeCount 내림차순으로 반환")
    void findTodayShotIdsRunsOnMysqlAndSortsByLikeCount() {
        Post lowLike = savePost("user-1", 5, true);
        Post highLike = savePost("user-2", 10, true);
        savePost("user-3", 99, false); // 미디어 없음 — 제외 대상
        em.flush();
        em.clear();

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();

        List<Long> ids = postRepository
                .findTodayShotIds(start, end, PageRequest.of(0, 10))
                .getContent();

        assertThat(ids).containsExactly(highLike.getId(), lowLike.getId());
    }
}
