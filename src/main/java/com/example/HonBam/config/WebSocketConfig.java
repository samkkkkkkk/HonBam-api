package com.example.HonBam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메세지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {



    // Websocket, Redis만 사용하는 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket에 연결할 수 있는 엔드포인트 설정
        registry.addEndpoint("/ws-chat")  // 클라이언트 WebSocket 엔드포인트
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();  // SockJS 사용
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 구독하는 주제(topic) 설정
        registry.enableSimpleBroker("/topic", "/queue");  // 메시지 브로커 prefix
        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix 설정
        registry.setApplicationDestinationPrefixes("/app");  // 클라이언트가 보낼 prefix
    }
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        //메세지 브로커 설정
//        // WebSocket, Redi만  이용해서 채팅 기능 구현하기
//        registry.enableSimpleBroker("/topic"); // 클라이언트 경로
//        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트 메세지 송신 경로
//
////        registry.enableStompBrokerRelay("/topic")
////                .setRelayHost("localhost") // Redis가 설치된 호스트
////                .setRelayPort(6379); // Redis 포트 (기본값 6379)
////        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트 메세지 송신 경로
//
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws") // WebSocket 연결 엔드포인트
//                .setAllowedOrigins("http://localhost:3000") // CORS 허용
//                .withSockJS(); // SockJS 지원
//    }
}
