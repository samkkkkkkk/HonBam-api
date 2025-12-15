package com.example.HonBam.userapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter @Getter
@ToString
public class NaverUserDTO {

    private int id;
    private String email;
    private String nickname;

    @JsonProperty("profile_image")
    private String profileImageUrl;


    public User toEntity(String accessToken) {
        return User.builder()
                .email(this.email)
                .userName(this.nickname)
                .password("password!")
                .accessToken(accessToken)
                .build();
    }



}