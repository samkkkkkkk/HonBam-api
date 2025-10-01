package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;

    public ChatMessageResponseDTO saveMessage(ChatMessageRequest request,
                                              String senderId,
                                              String senderName) {
        // UUID 기반으로 ChatRoom 조회
        ChatRoom room = chatRoomService.findByRoomUuid(request.getRoomId());

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(senderId)
                .senderName(senderName)
                .content(request.getContent())
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        return ChatMessageResponseDTO.builder()
                .roomId(room.getRoomUuid())  // UUID 반환
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .timestamp(saved.getTimestamp())
                .build();
    }

    // JPA Pageable 방식
    public List<ChatMessageResponseDTO> getMessagePageable(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messagePage =
                chatMessageRepository.findByRoomIdOrderByTimestampDesc(roomId, pageable);

        return mapToDTO(messagePage.getContent(), roomId);
    }

    // Native Query 방식
    public List<ChatMessageResponseDTO> getMessageNative(Long roomId, int page, int size) {
        int offset = (page - 1) * size;
        List<ChatMessage> messages =
                chatMessageRepository.findMessageWithPaging(roomId, size, offset);
        return mapToDTO(messages, roomId);
    }

    private List<ChatMessageResponseDTO> mapToDTO(List<ChatMessage> messages, Long roomId) {
        return messages.stream()
                .map(m -> ChatMessageResponseDTO.builder()
                        .roomId(m.getRoom().getRoomUuid())
                        .senderId(m.getSenderId())
                        .senderName(m.getSenderName())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unReadUserCount(chatRoomUserRepository
                                .countUnreadUsersForMessage(roomId, m.getTimestamp()))
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

}
