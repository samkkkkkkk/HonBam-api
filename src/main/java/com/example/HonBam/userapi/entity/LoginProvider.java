package com.example.HonBam.userapi.entity;

import lombok.Getter;

@Getter
public enum LoginProvider {
    LOCAL("local"),
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google");

    private final String registrationId;

    LoginProvider(String registrationId) {
        this.registrationId = registrationId;
    }

    public static LoginProvider from(String registrationId) {
        for (LoginProvider provider : values()) {
            if (provider.registrationId.equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }
}
