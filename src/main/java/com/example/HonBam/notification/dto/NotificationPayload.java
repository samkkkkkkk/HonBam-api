package com.example.HonBam.notification.dto;

import com.example.HonBam.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {

    private NotificationType type;
    private String receiverId;
    private String senderId;  
    private Long postId;    // 좋아요면 게시글 ID 없으면 null
    private Long commentId;
    private String chatRoomUuId;
    private Long chatMessageId;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> meta; // 확장 필드
}
