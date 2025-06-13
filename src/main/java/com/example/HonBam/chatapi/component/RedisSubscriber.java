//package com.example.HonBam.chatapi.component;
//
//import com.example.HonBam.chatapi.dto.ChatMessageDTO;
//import com.example.HonBam.chatapi.entity.ChatMessage;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.connection.MessageListener;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Component;
//
//import java.nio.charset.StandardCharsets;
//
//// RabbitMQ를 STOMP 브로커로 사용함에 따라, Redis Pub/Sub를 통한 메시지 수신 로직은 더 이상 사용되지 않을 수 있습니다.
//// 이 컴포넌트의 다른 용도가 없다면 삭제하거나, 관련 로직을 주석 처리합니다.
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class RedisSubscriber implements MessageListener {
//
////    @Lazy
////    private final SimpMessagingTemplate messagingTemplate; // RabbitMQ STOMP Relay 사용 시 직접 주입 불필요할 수 있음
////
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        log.warn("RedisSubscriber.onMessage called, but it might be deprecated due to RabbitMQ STOMP relay usage.");
////        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
////        ChatMessageDTO chatMessage = null;
////        try {
////            chatMessage = new ObjectMapper().readValue(payload, ChatMessageDTO.class);
////            // messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
////            log.info("실시간 채팅 메세지 (from Redis Pub/Sub - DEPRECATED?): {}", chatMessage);
////        } catch (Exception e) {
////            log.error("Error processing message in RedisSubscriber (DEPRECATED?)", e);
////            // throw new RuntimeException(e); // 에러 전파 방식 고려
////        }
//    }
////    @Override
////    public void onMessage(Message message, byte[] pattern) throws JsonProcessingException {
////        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
////        ChatMessage chatMessage = new ObjectMapper().readValue(payload, ChatMessage.class);
////        messagingTemplate.convertAndSend("/topic/chat/" + chatMessage.getRoomId(), chatMessage);
////
////    }
//}
