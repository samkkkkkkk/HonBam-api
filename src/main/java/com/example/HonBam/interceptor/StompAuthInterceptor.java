package com.example.HonBam.interceptor;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.WsTicket;
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

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final RedisTemplate<String, WsTicket> wsTicketRedisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String ticket = accessor.getFirstNativeHeader("ticket");

            if (ticket == null || ticket.isBlank()) {
                log.warn("STOMP CONNECT: 티켓 없음");
                throw new SecurityException("No ticket provided");
            }

            String key = "ws:ticket:" + ticket;
            log.info("레디스 키: {}",key);
            WsTicket wsTicket = wsTicketRedisTemplate.opsForValue().get(key);
            wsTicketRedisTemplate.delete(key);

            if (wsTicket == null) {
                log.warn("STOMP CONNECT: 유효하지 않은 티켓 {}", ticket);
                throw new SecurityException("Invalid or expired ticket");
            }


            if (wsTicket.getTicket() == null || !wsTicket.getTicket().equals(ticket)) {
                log.warn("STOMP CONNECT: 티켓 uuid 불일치 ticket: {} wsTicket: {}",ticket, wsTicket.getTicket());
            }

            if (wsTicket.getUserId() == null) {
                log.warn("STOMP CONNECT: 티켓에 userId 없음: {}", ticket);
                throw new SecurityException("Invalid payload ticket");
            }
            String userId = wsTicket.getUserId();

            TokenUserInfo userInfo = new TokenUserInfo(userId, null);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userInfo, null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            accessor.setUser(authentication);
            log.info("STOMP CONNECT 인증 성공: {}", userId);
        }

        return message;
    }
}
