package com.example.HonBam.notification.service;

import com.example.HonBam.notification.dto.NotificationPayload;
import com.example.HonBam.notification.entity.Notification;
import com.example.HonBam.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RedisTemplate<String, String> notificationRedisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    private String channel(String userId) {
        return "notification:" + userId;
    }

    public void publishAndPersist(NotificationPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            // Redis Pub/Sub
            notificationRedisTemplate.convertAndSend(channel(payload.getReceiverId()), json);

            // DB 저장
            Notification notification = Notification.builder()
                    .receiverId(payload.getReceiverId())
                    .notificationType(payload.getType())
                    .payloadJson(json)
                    .read(false)
                    .build();
            notificationRepository.save(notification);

            log.info("Notification published -> {}", channel(payload.getReceiverId()));
        } catch (Exception e) {
            log.error("알림 publish/persist 실패", e);
        }
    }
}
