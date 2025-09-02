package com.example.HonBam.auth.dto;

import lombok.*;

@Getter @Setter
@ToString
@AllArgsConstructor
public class ProviderProfile {
    private final String providerId;
    private final String email;
    private final String name;
    private final String nickname;
    private final String imageUrl;

}
