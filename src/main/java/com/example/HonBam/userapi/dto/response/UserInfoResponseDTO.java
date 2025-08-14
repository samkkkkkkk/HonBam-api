package com.example.HonBam.userapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponseDTO {
    private String id;
    private String userName;
    private String address;
    private String phoneNumber;
    private String nickname;
    private String userPay;
    private String email;

    public UserInfoResponseDTO(User user) {
        this.id = user.getId();
        this.userName = user.getUserName();
        this.address = user.getAddress();
        this.phoneNumber = user.getPhoneNumber();
        this.nickname = user.getNickname();
        this.userPay = String.valueOf(user.getUserPay());
        this.email = user.getEmail();
    }
}
