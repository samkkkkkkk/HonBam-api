package com.example.HonBam.chatapi.component;

import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        ChatMessageDTO chatMessage = null;
        try {
            chatMessage = new ObjectMapper().readValue(payload, ChatMessageDTO.class);
            messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
            log.info("실시간 채팅 메세지: {}", chatMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

//    @Override
//    public void onMessage(Message message, byte[] pattern) throws JsonProcessingException {
//        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
//        ChatMessage chatMessage = new ObjectMapper().readValue(payload, ChatMessage.class);
//        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
//
//    }


}
