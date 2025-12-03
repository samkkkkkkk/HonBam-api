package com.example.HonBam.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.web.bind.annotation.GetMapping;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_receiver_created",
                        columnList = "receiver_id, created_at"),
                @Index(name = "idx_notification_receiver_read",
                        columnList = "receiver_id, is_read")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", length = 36, nullable = false)
    private String receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;


    public void markRead() {
        if (!this.read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

}
