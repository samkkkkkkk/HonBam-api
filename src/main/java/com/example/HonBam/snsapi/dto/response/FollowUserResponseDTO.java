package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUserResponseDTO {

    private String userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime followedAt;

    public static FollowUserResponseDTO from(User user, String profileImageUrl, LocalDateTime followedAt) {
        return FollowUserResponseDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .followedAt(followedAt)
                .build();
    }
}
