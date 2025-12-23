package com.example.HonBam.snsapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LikeStatusResponse {
    private Boolean liked;
    private long likeCount;
}
