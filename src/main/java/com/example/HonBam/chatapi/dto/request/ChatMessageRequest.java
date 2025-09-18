package com.example.HonBam.chatapi.dto.request;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {
    private String roomId;    // UUID 기반 방 ID
    private String content;   // 메시지 내용
}
