package com.example.HonBam.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // FEED 관련 알림
    FOLLOW(NotificationCategory.FEED),
    LIKE(NotificationCategory.FEED),
    COMMENT(NotificationCategory.FEED),

    // CHAT 관련 알림
    CHAT_MESSAGE(NotificationCategory.CHAT),

    // SYSTEM 관련 알림
    SYSTEM_ANNOUNCEMENT(NotificationCategory.SYSTEM);

    private final NotificationCategory category;

}
