package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.component.ChatEventBroadcaster;
import com.example.HonBam.chatapi.dto.UnreadCountProjection;
import com.example.HonBam.chatapi.dto.UnreadSummaryProjection;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.event.ChatMessageSavedEvent;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.exception.ChatRoomNotFoundException;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PresignedUrlService presignedUrlService;
    private final ChatRoomUpdateService chatRoomUpdateService;
    private final MessageSaveCoreService messageSaveCoreService;

    public void saveMessage(ChatMessageRequest request,
                                              String senderId,
                                              String senderName) {

        // 메시지 저장
        ChatMessage saved = messageSaveCoreService.saveCore(request, senderId, senderName);
        ChatRoom room = saved.getRoom();

        // ChatRoom lastMessage 업데이트
        String preview = makePreview(request, saved);
        chatRoomUpdateService.updateLastMessage(
                room.getId(),
                preview,
                saved.getTimestamp(),
                saved.getId()
        );

        // presigned GET URL 생성
        String fileUrl = saved.getFileKey() != null
                ? presignedUrlService.generatePresignedGetUrl(saved.getFileKey())
                : null;

        // 비동기 처리 이벤트 발생
        eventPublisher.publishEvent(ChatMessageSavedEvent.of(saved));
    }

    private String makePreview(ChatMessageRequest request, ChatMessage saved) {
        switch (saved.getMessageType()) {
            case TEXT:
            case SYSTEM:
                return request.getContent();
            case FILE:
                return "[파일]";
            case IMAGE:
                return "[사진]";
            case VIDEO:
                return "[영상]";
            default:
                return "";
        }
    }

//    // JPA Pageable 방식
//    public List<ChatMessageResponseDTO> getMessagePageable(Long roomId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<ChatMessage> messagePage =
//                chatMessageRepository.findByRoomIdOrderByTimestampDesc(roomId, pageable);
//
//        return mapMessageWithUnreadCount(messagePage.getContent(), roomId);
//    }

//    // Native Query 방식
//    public List<ChatMessageResponseDTO> getMessageNative(Long roomId, int page, int size) {
//        int offset = (page - 1) * size;
//        List<ChatMessage> messages =
//                chatMessageRepository.findMessageWithPaging(roomId, size, offset);
//        return mapMessageWithUnreadCount(messages, roomId);
//    }

    // Cursor Before 방식
    @Transactional
    public List<ChatMessageResponseDTO> getMessagesCursor(String roomUuid, Long cursorId, int limit) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방을 찾을 수 없습니다."));

        List<ChatMessage> messages = chatMessageRepository.findMessagesBefore(room.getId(), cursorId, limit);
        return mapMessageWithUnreadCount(messages, room.getId(), room.getRoomUuid());
    }

    private List<ChatMessageResponseDTO> mapMessageWithUnreadCount(List<ChatMessage> messages, Long roomId, String roomUuid) {
        List<UnreadCountProjection> unreadResults = chatRoomUserRepository.countUnreadUsersForMessages(roomId);
        Map<Long, Long> unreadMap = unreadResults.stream()
                .collect(Collectors.toMap(
                        p -> p.getMessageId(),
                        p -> p.getUnreadCount()
                ));
        return convertMessagesWithUnreadMap(messages, unreadMap, roomUuid);
    }

    private List<ChatMessageResponseDTO> convertMessagesWithUnreadMap(
            List<ChatMessage> messages, Map<Long, Long> unreadMap, String roomUuid) {

        return messages.stream()
                .map(message -> {
                    String url = null;
                    if (message.getFileKey() != null) {
                        url = presignedUrlService.generatePresignedGetUrl(message.getFileKey());
                    }

                    return ChatMessageResponseDTO.from(message, roomUuid, unreadMap.getOrDefault(message.getId(), 0L), url);
                })
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getMessagesByRoom(String roomUuid) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new RuntimeException("해당 채팅방을 찾을 수 없습니다."));

        Long roomId = room.getId();

        // 최근 메시지 50개만
        List<ChatMessage> messages = chatMessageRepository.findMessageWithPaging(roomId, 50, 0);

        // 미읽음 카운트 매핑
        List<UnreadCountProjection> unreadResults = chatRoomUserRepository.countUnreadUsersForMessages(roomId);
        Map<Long, Long> unreadMap = unreadResults.stream()
                .collect(Collectors.toMap(
                        p -> p.getMessageId(),
                        p -> p.getUnreadCount()
                ));

        return messages.stream()
                .map(m -> {
                    String url = null;
                    if (m.getFileKey() != null) {
                        url = presignedUrlService.generatePresignedGetUrl(m.getFileKey());
                    }

                    return ChatMessageResponseDTO.builder()
                            .id(m.getId())
                            .roomUuid(roomUuid)
                            .senderId(m.getSenderId())
                            .senderName(m.getSenderName())
                            .messageType(m.getMessageType())
                            .content(m.getContent())
                            .fileUrl(url)
                            .fileName(m.getFileName())
                            .fileSize(m.getFileSize())
                            .timestamp(m.getTimestamp())
                            .unReadUserCount(unreadMap.getOrDefault(m.getId(), 0L))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
