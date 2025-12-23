package com.example.HonBam.snsapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowStatusResponse {
    private Boolean following;
    private long followerCount;
}
