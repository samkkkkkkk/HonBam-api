package com.example.HonBam.freeboardapi.dto.request;

import com.example.HonBam.freeboardapi.entity.Freeboard;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
import com.example.HonBam.userapi.entity.User;
import lombok.*;

@Getter @Setter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeboardCommentRequestDTO {
    private String comment;
    private Long id;

    public FreeboardComment toEntity(User user, Freeboard freeboard) {
        return FreeboardComment.builder()
                .writer(user.getUserName())
                .comment(this.comment)
                .freeboard(freeboard)
                .userId(user.getUserId())
                .build();
    }
}
