package com.example.HonBam.auth;

import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.UserPay;
import lombok.*;

@Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TokenUserInfo {

    private String email;
    private Role role;
    private UserPay userPay;
    private String nickName;
    private String address;
    private String phoneNumber;

}

