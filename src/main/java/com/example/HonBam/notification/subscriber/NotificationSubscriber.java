package com.example.HonBam.notification.subscriber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String topic = new String(message.getChannel());
            String body = new String(message.getBody());

            String userId = topic.substring(topic.lastIndexOf(":") + 1);
            // STOMP 구독 경로로 브로드캐스트
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, body);

            log.info("WS pushed -> /topic/notifications/{}", userId);
        } catch (Exception e) {
            log.error("Redis->WS 브릿지 실패", e);
        }

    }
}
