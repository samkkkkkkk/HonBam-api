package com.example.HonBam.notification.listener;

import com.example.HonBam.notification.dto.NotificationPayload;
import com.example.HonBam.notification.entity.NotificationType;
import com.example.HonBam.notification.event.ChatMessageCreateEvent;
import com.example.HonBam.notification.service.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.w3c.dom.events.Event;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageCreated(ChatMessageCreateEvent e) {

        List<String> targetUserIds = e.getTargetUserIds();
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.debug("ChatMessageCreateEvent targetUserIds 비어있음. roomId={}, messageId={}",
                    e.getRoomUuid(), e.getMessageId());
            return;
        }

        for (String receiverId : targetUserIds) {
            NotificationPayload payload = NotificationPayload.builder()
                    .type(NotificationType.CHAT_MESSAGE)
                    .receiverId(receiverId)
                    .senderId(e.getSenderId())
                    .chatRoomUuId(e.getRoomUuid())
                    .chatMessageId(e.getMessageId())
                    .message(trimPreview(e.getContent()))
                    .timestamp(LocalDateTime.now())
                    .build();

            notificationPublisher.publishAndPersist(payload);
        }
    }

    private String trimPreview(String content) {
        if (content == null) return null;
        int maxLength = 50;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
}
