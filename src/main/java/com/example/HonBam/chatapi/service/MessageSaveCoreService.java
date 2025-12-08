package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageSaveCoreService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage saveCore(ChatMessageRequest request, String senderId, String senderName) {

        ChatRoom room = chatRoomService.findByRoomUuid(request.getRoomUuid());

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(senderId)
                .senderName(senderName)
                .messageType(request.getMessageType())
                .content(request.getContent())
                .fileKey(request.getFileKey())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .build();

        return chatMessageRepository.save(message);
    }
}
