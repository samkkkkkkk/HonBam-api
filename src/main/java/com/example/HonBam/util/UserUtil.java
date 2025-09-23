package com.example.HonBam.util;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUtil {

    private final UserRepository userRepository;

    public User getUserOrThrow(TokenUserInfo tokenUserInfo) {
        return userRepository.findById(tokenUserInfo.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
    }

}
