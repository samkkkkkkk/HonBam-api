package com.example.HonBam.chatapi.service;

//import com.example.HonBam.chatapi.component.RedisPublisher;

import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.dto.ChatUserResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    //    private final RedisPublisher redisPublisher;
    private final UserRepository userRepository;

//    public void saveMessage(ChatMessageDTO chatMessageDTO) {
//        ChatMessage chatMessage = new ChatMessage(
//                null, chatMessageDTO.getRoomId(),
//                chatMessageDTO.getSender(), chatMessageDTO.getMessage(),
//                LocalDateTime.now()
//        );
//        chatMessageRepository.save(chatMessage);
//    }

    public List<ChatMessageDTO> getChatHistory(String roomId) {
        try {
            Long id = Long.valueOf(roomId);
            List<ChatMessage> messageList = chatMessageRepository.findByChatRoomRoomIdOrderByMessageTimeAsc(id);
            if (messageList.isEmpty()) {
                log.info("No messages found for roomId: {}", roomId);
                return Collections.emptyList();
            }
            return messageList
                    .stream()
                    .map(ChatMessageDTO::new)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Invalid roomId format: {}", roomId, e);
            return Collections.emptyList(); // 또는 예외 처리
        }

    }

//    public void sendMessage(ChatMessageDTO message) {
//        // Redis를 통해 메세지 발행
//        redisPublisher.publish("chat", message);
//    }

    // user목록 조회하기
    public List<ChatUserResponseDTO> findUsers(String currentId) {
        List<ChatUserResponseDTO> users = userRepository.findAll()
                .stream()
                .filter(user -> !user.getId().equals(currentId)) // 현재 사용자 제외
                .map(ChatUserResponseDTO::new)
                .collect(Collectors.toList());

        // Redis에 사용자 목록을 저장
        redisTemplate.opsForValue().set("chat_users", users);
        return users;
    }
}

