package com.example.HonBam.notification.dto;

import com.example.HonBam.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {

    private NotificationType type;
    private String receiverId;
    private String senderId;  
    private Long postId;    // 좋아요면 게시글 ID 없으면 null
    private LocalDateTime timestamp;
    private Map<String, Objects> meta; // 확장 필드
}
