package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponse;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse saveMessage(ChatMessageRequest request,
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

        return ChatMessageResponse.builder()
                .roomId(room.getRoomUuid())  // UUID 반환
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .content(saved.getContent())
                .timestamp(saved.getTimestamp())
                .build();
    }
}
