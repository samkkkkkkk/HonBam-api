package com.example.HonBam.chatapi.event;

import com.example.HonBam.chatapi.component.ChatEventBroadcaster;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.notification.event.ChatMessageCreateEvent;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageAsyncHandler {

    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventBroadcaster chatEventBroadcaster;
    private final ApplicationEventPublisher eventPublisher;
    private final PresignedUrlService presignedUrlService;

    @Async
    @TransactionalEventListener
    public void onChatMessageSaved(ChatMessageSavedEvent event) {

        try {
            Long messageId = event.getMessageId();
            Long roomId = event.getRoomId();
            String roomUuid = event.getRoomUuid();
            String senderId = event.getSenderId();

            // 메시지 조회
            ChatMessage message = chatMessageRepository.findById(messageId)
                    .orElse(null);
            if (message == null) return;

            ChatRoom room = message.getRoom();

            String fileUrl = message.getFileKey() != null
                    ? presignedUrlService.generatePresignedGetUrl(message.getFileKey())
                    : null;

            // unreadCount 계산
            long unreadCount = chatRoomUserRepository.countUnreadUsersForMessage(room.getId(), messageId, senderId);

            // 참여자 조회
            List<ChatRoomUser> participants = chatRoomUserRepository.findByRoom(room);

            // summary 정보 계산
            Map<String, Long> unreadMap = participants.stream()
                    .collect(Collectors.toMap(
                            cru -> cru.getUser().getId(),
                            cru -> chatMessageRepository.countUnreadMessagesForRoomAndUser(
                                    roomId, cru.getUser().getId())
                    ));

            // 메시지 브로드캐스트
            chatEventBroadcaster.sendChatMessage(roomUuid, ChatMessageResponseDTO.from(message, unreadCount, fileUrl));

            // summary 브로드캐스트
            chatEventBroadcaster.broadcastRoomSummaryForParticipants(room, participants, unreadMap, senderId);

            // 알림 이벤트 발행
            List<String> targetUserIds = participants.stream()
                    .map(cru -> cru.getUser().getId())
                    .filter(uid -> !uid.equals(senderId))
                    .collect(Collectors.toUnmodifiableList());

            if (!targetUserIds.isEmpty()) {
                eventPublisher.publishEvent(
                        ChatMessageCreateEvent.of(message, targetUserIds)
                );
            }
        } catch (Exception e) {
            log.error("Async ChatMessageSavedEvent 처리 실패. messageId={}", event.getMessageId(), e);
        }
    }
}
