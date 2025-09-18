package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.WsTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class WsTicketController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/ws-ticket")
    public ResponseEntity<?> issueTicket(@AuthenticationPrincipal TokenUserInfo user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 30초짜리 티켓 생성
        String ticket = UUID.randomUUID().toString();
        WsTicket wsTicket = WsTicket.builder()
                .ticket(ticket)
                .userId(user.getUserId())
                .expiredAt(Instant.now().plusSeconds(30))
                .build();

        // Redis에 저장 (30초 TTL)
        String key = "ws:ticket:" + ticket;
        redisTemplate.opsForValue().set(key, wsTicket.getUserId(), 30, TimeUnit.SECONDS);
        log.info("티켓 발급: {} -> {}", ticket, user.getUserId());

        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}