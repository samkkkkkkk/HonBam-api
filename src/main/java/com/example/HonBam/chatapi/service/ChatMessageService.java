package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.UnreadCountProjection;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMedia;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.event.ChatMessageSavedEvent;
import com.example.HonBam.chatapi.repository.ChatMediaRepository;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.exception.ChatRoomNotFoundException;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
    private final ChatMediaRepository chatMediaRepository;

    @Transactional
    public void saveMessage(ChatMessageRequest request, String senderId, String senderName) {

        // 메시지 저장
        ChatMessage saved = messageSaveCoreService.saveCore(request, senderId, senderName);
        ChatRoom room = saved.getRoom();

        // ChatRoom lastMessage 업데이트
        String preview = makePreview(request, saved);
        chatRoomUpdateService.updateLastMessage(room.getId(),
                preview,
                saved.getTimestamp(),
                saved.getId()
        );

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
        return mapMessageWithDetails(messages, room.getId(), room.getRoomUuid());
    }

    private List<ChatMessageResponseDTO> mapMessageWithDetails(List<ChatMessage> messages, Long roomId, String roomUuid) {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        // 안읽음 카운트 조회
        Map<Long, Long> unreadMap = getUnreadCountMap(roomId);

        // 미디어 파일 일괄 조회
        List<ChatMedia> allMedias = chatMediaRepository.findByMessageIn(messages);

        Map<Long, List<ChatMedia>> mediaMap = allMedias.stream()
                .collect(Collectors.groupingBy(cm -> cm.getMessage().getId()));

        // 4. DTO 변환 및 조립
        return messages.stream()
                .map(message -> {
                    long unreadCount = unreadMap.getOrDefault(message.getId(), 0L);
                    // 해당 메시지의 미디어 리스트를 가져옴 (없으면 빈 리스트)
                    List<ChatMedia> medias = mediaMap.getOrDefault(message.getId(), Collections.emptyList());

                    return convertToDTO(message, roomUuid, unreadCount, medias);
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

        return mapMessageWithDetails(messages, roomId, roomUuid);
    }

    // 안읽음 카운트 Map 생성 헬퍼
    private Map<Long, Long> getUnreadCountMap(Long roomId) {
        return chatRoomUserRepository.countUnreadUsersForMessages(roomId).stream()
                .collect(Collectors.toMap(
                        UnreadCountProjection::getMessageId,
                        UnreadCountProjection::getUnreadCount
                ));
    }

    // 단일 메시지 DTO 변환 (Presigned URL 생성 포함)
    private ChatMessageResponseDTO convertToDTO(ChatMessage message, String roomUuid, long unreadCount, List<ChatMedia> medias) {

        // ChatMedia -> FileInfoDTO 변환 (여기서 Presigned URL을 생성합니다)
        List<ChatMessageResponseDTO.FileInfoDTO> fileDtos = medias.stream()
                .map(cm -> ChatMessageResponseDTO.FileInfoDTO.builder()
                        .mediaId(cm.getMedia().getId())
                        .fileUrl(presignedUrlService.generatePresignedGetUrl(cm.getMedia().getFileKey())) // URL 생성
                        .fileName(cm.getMedia().getFileKey())
                        .contentType(cm.getMedia().getContentType())
                        .fileSize(cm.getMedia().getFileSize())
                        .build())
                .collect(Collectors.toList());

        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .roomUuid(roomUuid)
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .unReadUserCount(unreadCount)
                .files(fileDtos) // [중요] 파일 리스트 세팅
                .build();
    }


}
