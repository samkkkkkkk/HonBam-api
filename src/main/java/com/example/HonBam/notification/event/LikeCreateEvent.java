package com.example.HonBam.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeCreateEvent {
    private final String likeId;
    private final Long postId;
    private final String postAuthorId;
}
