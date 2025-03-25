package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.component.RedisPublisher;
import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisPublisher redisPublisher;

//    public void saveMessage(ChatMessageDTO chatMessageDTO) {
//        ChatMessage chatMessage = new ChatMessage(
//                null, chatMessageDTO.getRoomId(),
//                chatMessageDTO.getSender(), chatMessageDTO.getMessage(),
//                LocalDateTime.now()
//        );
//        chatMessageRepository.save(chatMessage);
//    }

    public List<ChatMessage> getChatHistory(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    public void sendMessage(ChatMessageDTO message) {
        // Redis를 통해 메세지 발행
        redisPublisher.publish("chat", message);
    }
}
