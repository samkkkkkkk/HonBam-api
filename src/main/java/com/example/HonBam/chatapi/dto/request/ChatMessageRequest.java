package com.example.HonBam.chatapi.dto.request;

import com.example.HonBam.chatapi.entity.MessageType;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {
    private String roomUuid;    // UUID 기반 방 ID
    private MessageType messageType;

    private String content;   // 메시지 내용

    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
