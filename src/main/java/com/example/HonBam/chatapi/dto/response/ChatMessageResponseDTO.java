package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.chatapi.entity.ChatMessage;
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

    public static ChatMessageResponseDTO from(ChatMessage message, String roomUuid, Long unReadUserCount, String fileUrl) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .roomUuid(roomUuid)
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(fileUrl)
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .timestamp(message.getTimestamp())
                .unReadUserCount(unReadUserCount)
                .build();
    }
}
