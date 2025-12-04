package com.example.HonBam.notification.subscriber;

import com.example.HonBam.notification.dto.NotificationPayload;
import com.example.HonBam.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String topic = new String(message.getChannel());
            String body = new String(message.getBody());

            NotificationPayload dto = objectMapper.readValue(body, NotificationPayload.class);
            String userId = topic.substring(topic.lastIndexOf(":") + 1);

            String destination = "/topic/notifications/" + userId;

            messagingTemplate.convertAndSend(destination, Map.of(
                    "type", dto.getType(),
                    "data", dto
            ));

            log.info("Successfully pushed to {}", destination);
        } catch (Exception e) {
            log.error("Redis->WS 브릿지 실패", e);
        }

    }
}
