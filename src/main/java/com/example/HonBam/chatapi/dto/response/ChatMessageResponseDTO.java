package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.chatapi.entity.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDTO {

    private String roomId; // 채팅방 ID
    private String senderId; // 메시지를 보낸 사용자의 ID (기존 sender 필드 대체)
    private String senderName; // 메시지를 보낸 사용자의 이름/닉네임 (추가)
    private String message; // 메시지 내용 (기존 content 필드 대체)
    private LocalDateTime timestamp; // 메시지 타임스탬프 (서버에서 설정 또는 클라이언트 값 사용 후 서버가 최종 설정)

    public ChatMessageResponseDTO(ChatMessage chatMessage) {
        this.roomId = chatMessage.getChatRoom().getRoomId().toString();
        this.senderId = chatMessage.getSender().getId();
        this.senderName = chatMessage.getSender().getNickName(); // 닉네임
        this.message = chatMessage.getMessage();
        this.timestamp = chatMessage.getMessageTime();
    }

}
