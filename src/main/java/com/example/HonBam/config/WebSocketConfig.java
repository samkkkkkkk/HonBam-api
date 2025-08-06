package com.example.HonBam.config;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메세지 브로커 활성화
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenProvider tokenProvider; // TokenProvider 주입

    // 생성자를 통해 TokenProvider 주입
    public WebSocketConfig(TokenProvider tokenProvider
            /*, 다른 @Value 필드들도 생성자 주입으로 변경 가능 */
    ) {
        this.tokenProvider = tokenProvider;
    }

    // application.yml에서 RabbitMQ STOMP Relay 설정 값 주입
    @Value("${app.rabbitmq.stomp.host}")
    private String relayHost;

    @Value("${app.rabbitmq.stomp.port}")
    private int relayPort;

    @Value("${app.rabbitmq.stomp.username}")
    private String clientLogin;

    @Value("${app.rabbitmq.stomp.password}")
    private String clientPasscode;

    @Value("${app.rabbitmq.stomp.virtual-host}")
    private String virtualHost; // RabbitMQ Virtual Host 설정


//     Websocket, Redis만 사용하는 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket에 연결할 수 있는 엔드포인트 설정
        registry.addEndpoint("/ws-chat")  // 클라이언트 WebSocket 엔드포인트
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();  // SockJS 사용
    }

    //    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        // 메시지를 구독하는 주제(topic) 설정
//        registry.enableSimpleBroker("/topic", "/queue");  // 메시지 브로커 prefix
//        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix 설정
//        registry.setApplicationDestinationPrefixes("/app");  // 클라이언트가 보낼 prefix
//    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // RabbitMQ를 STOMP 브로커 릴레이로 사용 설정
        // "/topic", "/queue" 등의 prefix를 가진 메시지를 Redis를 통해 브로드캐스팅/라우팅합니다.
        registry.enableStompBrokerRelay("/topic", "/queue", "/exchange") // /exchange도 추가하여 유연성 확보
                .setRelayHost(relayHost) // RabbitMQ 서버 호스트 (yml에서 주입)
                .setRelayPort(relayPort)       // RabbitMQ STOMP 플러그인 기본 포트 (yml에서 주입)
                .setClientLogin(clientLogin)   // RabbitMQ STOMP 사용자 (yml에서 주입)
                .setClientPasscode(clientPasscode) // RabbitMQ STOMP 비밀번호 (yml에서 주입)
                .setVirtualHost(virtualHost) // RabbitMQ Virtual Host 설정 (yml에서 주입)
                .setSystemLogin("guest")   // RabbitMQ 시스템 연결용 (필요시)
                .setSystemPasscode("guest");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 prefix 설정
        registry.setApplicationDestinationPrefixes("/app");  // 클라이언트가 메시지를 보낼 때 사용하는 prefix (@MessageMapping 경로 앞부분)
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization"); // 또는 클라이언트가 토큰을 보내는 헤더 이름
                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        String token = authorizationHeader.substring(7);
                        try {
                            TokenUserInfo userInfo = tokenProvider.validateAndGetTokenUserInfo(token);
                            log.info("STOMP User Authenticated: {}", userInfo.getEmail());
                            // Spring Security Authentication 객체 생성
                            // 여기서 authorities는 필요에 따라 설정
                            // Principal.getName()이 userId를 반환하도록 userInfo.getUserId()를 principal로 사용
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userInfo.getEmail(), null, /* authorities */ null);
                            accessor.setUser(authentication);
                        } catch (Exception e) {
                            // 토큰 검증 실패 처리
                            log.error("STOMP Connect Token Validation Failed: {}", e.getMessage());
                            log.error("STOMP Connect Token Validation Failed: " + e.getMessage(), e);
                            // Throw a security-related exception. Spring's STOMP handling should
                            // convert this into an ERROR frame and close the connection.
                            throw new org.springframework.security.access.AccessDeniedException("Invalid token for STOMP connection.", e);
                        }
                    }
                }
                return message;
            }
        });
    }

}


