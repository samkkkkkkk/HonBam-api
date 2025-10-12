package com.example.HonBam.chatapi.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);

            String roomUuid = (String) payload.get("roomUuid");
            Long messageId = ((Number) payload.get("messageId")).longValue();
            Long unread = ((Number) payload.get("unReadUserCount")).longValue();

            // 다른 서버에서도 RabbitMQ STOMP 브로커로 동일하게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat.room." + roomUuid + ".read",
                    payload
            );

            log.info("[READ EVENT SYNC] room={}, msgId={}, unread={}", roomUuid, messageId, unread);

        } catch (Exception e) {
            log.error("[READ EVENT SYNC ERROR] Redis 메시지 처리 실패", e);
        }
    }
}
