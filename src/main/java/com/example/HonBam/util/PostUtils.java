package com.example.HonBam.util;

import com.example.HonBam.upload.entity.Media;
import com.example.HonBam.upload.service.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostUtils {

    private final PresignedUrlService presignedUrlService;

    //
    public String buildMediaUrl(Media media) {
        return presignedUrlService.generatePresignedGetUrl(media.getFileKey());
    }


    // 프로필 URL
//    public String buildProfileUrl(User author) {
//        if (author.getLoginProvider() != LoginProvider.LOCAL) {
//            return author.getProfileImg();
//        }
//
//        if (author.getProfileImg() == null) {
//            return "/default-profile.png";
//        }
//
//        return "uploads/" + author.getProfileImg();
//    }
}