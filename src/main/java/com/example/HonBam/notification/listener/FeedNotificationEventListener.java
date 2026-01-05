package com.example.HonBam.notification.listener;

import com.example.HonBam.notification.dto.NotificationPayload;
import com.example.HonBam.notification.entity.Notification;
import com.example.HonBam.notification.entity.NotificationType;
import com.example.HonBam.notification.event.FollowerCreatedEvent;
import com.example.HonBam.notification.event.LikeCreateEvent;
import com.example.HonBam.notification.repository.NotificationRepository;
import com.example.HonBam.notification.service.NotificationPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedNotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    @Async
    @TransactionalEventListener
    public void onFollowCreated(FollowerCreatedEvent event) {
        NotificationPayload payload = NotificationPayload.builder()
                .type(NotificationType.FOLLOW)
                .receiverId(event.getFollowingId())
                .senderId(event.getFollowerId())
                .timestamp(LocalDateTime.now())
                .build();

        notificationPublisher.publishAndPersist(payload);

    }

    @Async
    @TransactionalEventListener
    public void onLikeCreated(LikeCreateEvent e) {
        NotificationPayload payload = NotificationPayload.builder()
                .type(NotificationType.LIKE)
                .receiverId(e.getPostAuthorId())
                .senderId(e.getLikeId())
                .postId(e.getPostId())
                .timestamp(LocalDateTime.now())
                .build();

        notificationPublisher.publishAndPersist(payload);
    }
}
