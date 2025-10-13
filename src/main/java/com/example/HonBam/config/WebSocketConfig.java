package com.example.HonBam.config;

import com.example.HonBam.auth.TokenProvider;
import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.interceptor.StompAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenProvider tokenProvider;
    private final StompAuthInterceptor stompAuthInterceptor;

    public WebSocketConfig(TokenProvider tokenProvider, StompAuthInterceptor stompAuthInterceptor) {
        this.tokenProvider = tokenProvider;
        this.stompAuthInterceptor = stompAuthInterceptor;
    }

    @Value("${app.rabbitmq.stomp.host}")
    private String relayHost;

    @Value("${app.rabbitmq.stomp.port}")
    private int relayPort;

    @Value("${app.rabbitmq.stomp.username}")
    private String clientLogin;

    @Value("${app.rabbitmq.stomp.password}")
    private String clientPasscode;

    @Value("${app.rabbitmq.stomp.virtual-host}")
    private String virtualHost;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000") // 하나만 사용
                .withSockJS()
                .setSessionCookieNeeded(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue", "/exchange")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                .setVirtualHost(virtualHost)
                .setSystemLogin("guest")
                .setSystemPasscode("guest");

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}