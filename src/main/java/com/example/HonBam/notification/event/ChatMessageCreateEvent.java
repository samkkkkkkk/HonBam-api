package com.example.HonBam.notification.event;

import com.example.HonBam.chatapi.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ChatMessageCreateEvent {
    private final Long messageId;
    private final String roomUuid;
    private final String senderId;
    private final List<String> targetUserIds;
    private final String content;
    private final LocalDateTime createdAt;


    public static ChatMessageCreateEvent of(ChatMessage message, List<String> targetUserIds) {
        return ChatMessageCreateEvent.builder()
                .messageId(message.getId())
                .roomUuid(message.getRoom().getRoomUuid())
                .senderId(message.getSenderId())
                .targetUserIds(targetUserIds)
                .content(message.getContent())
                .createdAt(message.getTimestamp())
                .build();
    }

}
