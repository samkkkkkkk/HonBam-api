package com.example.HonBam.upload.entity;

import lombok.Getter;

@Getter
public enum MediaPurpose {
    PROFILE("profile"),
    POST("post"),
    CHAT("chat");

    private final String prefix;

    MediaPurpose(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
