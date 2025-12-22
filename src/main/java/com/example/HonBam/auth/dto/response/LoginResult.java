package com.example.HonBam.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {
    private final String accessToken;
    private final String refreshToken;

//    public LoginResult(String accessToken, String refreshToken) {
//        this.accessToken = accessToken;
//        this.refreshToken = refreshToken;
//    }
}
