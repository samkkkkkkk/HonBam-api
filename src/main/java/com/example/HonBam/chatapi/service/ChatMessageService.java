package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.component.ChatEventBroadcaster;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatEventBroadcaster broadcaster;

    public ChatMessageResponseDTO saveMessage(ChatMessageRequest request,
                                              String senderId,
                                              String senderName) {
        // UUID 기반으로 ChatRoom 조회
        ChatRoom room = chatRoomService.findByRoomUuid(request.getRoomUuid());

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(senderId)
                .senderName(senderName)
                .content(request.getContent())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("메시지 저장: {}", message);

        room.updateLastMessage(request.getContent(), message.getTimestamp(), message.getId());

        try {
            chatRoomRepository.save(room);
        } catch (DataAccessException e) {
            log.error("[MESSAGE SAVE] DB error: {} ", e.getMessage());
            throw new RuntimeException("메시지 저장 중 오류 발생", e);
        }

        // 안 읽은 메시지 수 계산
        long unreadCount = chatRoomUserRepository.countUnreadUsersForMessage(
                room.getId(),
                saved.getId(),
                senderId
        );

        ChatMessageResponseDTO response = ChatMessageResponseDTO.builder()
                .id(saved.getId())
                .roomUuid(room.getRoomUuid())  // UUID 반환
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .timestamp(saved.getTimestamp())
                .unReadUserCount(unreadCount)
                .build();

        // 채팅방 내부 메시지 브로드캐스트
        broadcaster.sendChatMessage(room.getRoomUuid(), response);

        // 각 참여자의 unreadCount를 다시 계산해서 summary 브로드캐스트
        List<ChatRoomUser> participants = chatRoomUserRepository.findByRoom(room);
        Map<String, Long> unreadMap = participants.stream().collect(
                Collectors.toMap(
                        cru -> cru.getUser().getId(),
                        cru -> chatMessageRepository.countUnreadMessagesForRoomAndUser(room.getId(), cru.getUser().getId()))
        );

        broadcaster.broadcastRoomSummaryForParticipants(room, participants, unreadMap, senderId);

        return response;

    }

    // JPA Pageable 방식
    public List<ChatMessageResponseDTO> getMessagePageable(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage =
                chatMessageRepository.findByRoomIdOrderByTimestampDesc(roomId, pageable);

        return mapMessageWithUnreadCount(messagePage.getContent(), roomId);
    }

    // Native Query 방식
    public List<ChatMessageResponseDTO> getMessageNative(Long roomId, int page, int size) {
        int offset = (page - 1) * size;
        List<ChatMessage> messages =
                chatMessageRepository.findMessageWithPaging(roomId, size, offset);
        return mapMessageWithUnreadCount(messages, roomId);
    }

    // Cursor Before 방식
    @Transactional
    public List<ChatMessageResponseDTO> getMessagesCursor(String roomUuid, LocalDateTime cursorTime, int limit) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        List<ChatMessage> messages = chatMessageRepository.findMessagesBefore(room.getId(), cursorTime, limit);
        return mapMessageWithUnreadCount(messages, room.getId());
    }

    private List<ChatMessageResponseDTO> mapMessageWithUnreadCount(List<ChatMessage> messages, Long roomId) {
        List<Object[]> unreadResults = chatRoomUserRepository.countUnreadUsersForMessages(roomId);
        Map<Long, Long> unreadMap = unreadResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        return convertMessagesWithUnreadMap(messages, unreadMap);
    }

    private List<ChatMessageResponseDTO> convertMessagesWithUnreadMap(List<ChatMessage> messages, Map<Long, Long> unreadMap) {
        return messages.stream()
                .map(m -> ChatMessageResponseDTO.builder()
                        .id(m.getId())
                        .roomUuid(m.getRoom().getRoomUuid())
                        .senderName(m.getSenderName())
                        .senderId(m.getSenderId())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unReadUserCount(unreadMap.getOrDefault(m.getId(), 0L))
                        .build())
                .collect(Collectors.toList());
    }

    public void comparePerformance(Long roomId, int page, int size) {

        StopWatch stopWatch = new StopWatch();

        // JPA Pageable
        stopWatch.start("JPA pageable");
        List<ChatMessageResponseDTO> pageableResult = getMessagePageable(roomId, page, size);
        stopWatch.stop();

        // Native Query
        stopWatch.start("Native Query");
        int offset = page * size;
        List<ChatMessageResponseDTO> nativeResult = getMessageNative(roomId, offset, size);
        stopWatch.stop();

        // 결과 출력
        System.out.println(stopWatch.prettyPrint());

    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getMessagesByRoom(String roomUuid) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new RuntimeException("해당 채팅방을 찾을 수 없습니다."));

        Long roomId = room.getId();

        // 최근 메시지 50개만 (성능 고려)
        List<ChatMessage> messages = chatMessageRepository.findMessageWithPaging(roomId, 50, 0);

        // 미읽음 카운트 매핑
        List<Object[]> unreadResults = chatRoomUserRepository.countUnreadUsersForMessages(roomId);
        Map<Long, Long> unreadMap = unreadResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        return messages.stream()
                .map(m -> ChatMessageResponseDTO.builder()
                        .id(m.getId())
                        .roomUuid(roomUuid)
                        .senderId(m.getSenderId())
                        .senderName(m.getSenderName())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unReadUserCount(unreadMap.getOrDefault(m.getId(), 0L))
                        .build())
                .collect(Collectors.toList());
    }
}
