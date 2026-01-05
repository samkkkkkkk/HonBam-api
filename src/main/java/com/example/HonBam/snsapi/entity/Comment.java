package com.example.HonBam.snsapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sns_comment")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false, length = 36)
    private String authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "parent_id")
    private Long parentId; // 대댓글 확장용

    public void editContent(String newContent) {
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용은 비어 있을 수 없습니다.");
        }
        this.content = newContent.trim();
    }

}
