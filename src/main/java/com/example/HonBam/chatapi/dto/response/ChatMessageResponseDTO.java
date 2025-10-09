package com.example.HonBam.chatapi.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDTO {
    private String roomUuid;    // UUID만 노출
    private String senderId;
    private String senderName;
    private String content;
    private LocalDateTime timestamp;
    private Long unReadUserCount;
}
