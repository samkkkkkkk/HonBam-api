package com.example.HonBam.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowerCreatedEvent {
    private final String followerId; // 누가
    private final String followingId; // 누구를 팔로우 했는지 (알림 수신자)

}
