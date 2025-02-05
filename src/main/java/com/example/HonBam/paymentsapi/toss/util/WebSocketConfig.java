package com.example.HonBam.paymentsapi.toss.util;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //메세지 브로커 설정
        registry.enableSimpleBroker("/topic"); // 클라이언트 경로
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트 메세지 송신 경로

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // WebSocket 연결 엔드포인트
                .setAllowedOrigins("http://localhost:3000") // CORS 허용
                .withSockJS(); // SockJS 지원
    }
}
