package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatMedia;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMediaRepository;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageSaveCoreService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMediaRepository chatMediaRepository;

    private final UploadService uploadService;

    @Transactional
    public ChatMessage saveCore(ChatMessageRequest request, String senderId, String senderName) {

        ChatRoom room = chatRoomService.findByRoomUuid(request.getRoomUuid());

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .senderId(senderId)
                .senderName(senderName)
                .messageType(request.getMessageType())
                .content(request.getContent())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        if (request.getFileKey() != null) {
            Media savedMedia = uploadService.createMedia(
                    senderId,
                    request.getFileKey(),
                    MediaPurpose.CHAT
            );

            ChatMedia chatMedia = ChatMedia.builder()
                    .message(savedMessage)
                    .media(savedMedia)
                    .build();
        }
        return savedMessage;
    }
}
