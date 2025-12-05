package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.chatapi.entity.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDTO {
    private Long id;
    private String roomUuid;    // UUID만 노출
    private String senderId;
    private String senderName;
    private MessageType messageType;
    private String content;

    private String fileUrl;
    private String fileName;
    private Long fileSize;

    private LocalDateTime timestamp;
    private Long unReadUserCount;
}
