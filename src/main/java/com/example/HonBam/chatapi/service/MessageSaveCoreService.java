package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatMedia;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMediaRepository;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.entity.MediaPurpose;
import com.example.HonBam.upload.repository.MediaRepository;
import com.example.HonBam.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class MessageSaveCoreService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMediaRepository chatMediaRepository;
    private final MediaRepository mediaRepository;

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

        List<Long> mediaIds = request.getMediaIds();
        if (mediaIds != null && !mediaIds.isEmpty()) {

            List<Long> uniqueIds = mediaIds.stream().distinct().collect(toList());
            List<Media> medias = mediaRepository.findAllById(uniqueIds);

            // 2-1) 요청한 개수와 조회된 개수 일치 검증
            if (medias.size() != uniqueIds.size()) {
                throw new IllegalArgumentException("존재하지 않는 첨부 파일이 포함되어 있습니다.");
            }

            // 소유자/목적 검증
            for (Media m : medias) {
                if (!senderId.equals(m.getUploaderId())) {
                    throw new IllegalArgumentException("본인 파일만 첨부할 수 있습니다.");
                }
                if (m.getMediaPurpose() != MediaPurpose.CHAT) {
                    throw new IllegalArgumentException("채팅 첨부용 파일만 허용됩니다.");
                }
            }

            for (Media m : medias) {
                chatMediaRepository.save(ChatMedia.builder()
                        .message(savedMessage)
                        .media(m)
                        .build());
            }

        }

        return savedMessage;
    }
}
