package com.example.HonBam.chatapi.dto;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private String roomId; // 채팅방 ID
    private String sender; // 메시지를 보낸 사용자
    private String content; // 메시지 내용
}
