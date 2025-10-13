package com.example.HonBam.interceptor;

import com.example.HonBam.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String ticket = accessor.getFirstNativeHeader("ticket");

            if (ticket != null) {
                String key = "ws:ticket:" + ticket;
                Object userId = redisTemplate.opsForValue().get(key);
                redisTemplate.delete(key);
                if (userId != null) {
                    // 티켓은 일회성이므로 즉시 제거
                    redisTemplate.delete(key);

                    TokenUserInfo userInfo = new TokenUserInfo(userId.toString(), null);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userInfo, null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    accessor.setUser(authentication);

                    log.info("STOMP CONNECT 인증 성공: {}", userId);
                } else {
                    log.warn("STOMP CONNECT: 유효하지 않은 티켓 {}", ticket);
                    throw new SecurityException("Invalid or expired ticket");
                }
            } else {
                log.warn("STOMP CONNECT: 티켓 없음");
                throw new SecurityException("No ticket provided");
            }
        }
        return message;
    }
}
