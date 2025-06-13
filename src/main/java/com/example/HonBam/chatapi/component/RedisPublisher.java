//package com.example.HonBam.chatapi.component;
//
//import com.example.HonBam.chatapi.dto.ChatMessageDTO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class RedisPublisher {
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    public void publish(String topic, ChatMessageDTO message) {
//        redisTemplate.convertAndSend(topic, message);
//    }
//}
