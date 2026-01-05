package com.example.HonBam.snsapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollowResponseDTO {
    private String userId;
    private String nickname;
    private String profileImageUrl;
    private long followerCount;
    private long followingCount;
    private boolean following;
    private long postCount;

    public static UserFollowResponseDTO from(String userId, String nickname, String profileImageUrl, long followerCount, long followingCount, boolean following, long postCount) {
        return UserFollowResponseDTO.builder()
                .userId(userId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .following(following)
                .postCount(postCount)
                .build();

    }
}
