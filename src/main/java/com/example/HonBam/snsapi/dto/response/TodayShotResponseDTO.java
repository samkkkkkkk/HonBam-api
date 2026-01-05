package com.example.HonBam.snsapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodayShotResponseDTO {

    private Long postId;
    private String firstImageUrl;
    private List<String> imageUrls;
    private String content;
    private int likeCount;
    private String authorNickname;
    private String authorProfileUrl;
}
