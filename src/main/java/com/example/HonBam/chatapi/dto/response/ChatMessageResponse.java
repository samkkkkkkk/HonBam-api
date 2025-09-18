package com.example.HonBam.chatapi.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String roomId;    // UUID만 노출
    private String senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
}
