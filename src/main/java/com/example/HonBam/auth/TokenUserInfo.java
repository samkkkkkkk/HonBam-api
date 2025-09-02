package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.Role;
import lombok.*;

@Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TokenUserInfo {
    private String userId;
    private Role role;
}

