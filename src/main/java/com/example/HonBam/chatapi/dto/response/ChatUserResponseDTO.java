package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import lombok.Getter;

@Getter
public class ChatUserResponseDTO {

    private String id;
    private String nickname;
    private String profileImageUrl;

    public ChatUserResponseDTO(User user) {
        this.id = user.getId();
        this.nickname = user.getUserId();
        this.profileImageUrl = user.getProfileImg();
    }
}
