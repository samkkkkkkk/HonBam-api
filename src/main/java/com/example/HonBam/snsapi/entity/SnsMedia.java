package com.example.HonBam.snsapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sns_media")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnsMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // S3 object key
    @Column(nullable = false, length = 255)
    private String fileKey;

    // 클라이언트에서 쓸 URL
    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
