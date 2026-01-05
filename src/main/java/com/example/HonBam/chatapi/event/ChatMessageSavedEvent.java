package com.example.HonBam.chatapi.event;

import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageSavedEvent {
    private Long messageId;
    private Long roomId;
    private String roomUuid;
    private String senderId;

    public static ChatMessageSavedEvent of(ChatMessage saved) {
        ChatRoom room = saved.getRoom();
        return ChatMessageSavedEvent.builder()
                .messageId(saved.getId())
                .roomId(room.getId())
                .roomUuid(room.getRoomUuid())
                .senderId(saved.getSenderId())
                .build();
    }
}
