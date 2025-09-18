package com.example.HonBam.chatapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomUserResponse {
    private String userId;
    private String nickname;
    private String email;

    public static ChatRoomUserResponse from(User user) {
        return ChatRoomUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }
}
