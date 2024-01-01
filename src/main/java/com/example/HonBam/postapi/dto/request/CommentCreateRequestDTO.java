package com.example.HonBam.postapi.dto.request;

import com.example.HonBam.freeboardapi.entity.Freeboard;
import com.example.HonBam.freeboardapi.entity.FreeboardComment;
import com.example.HonBam.postapi.entity.Comment;
import com.example.HonBam.postapi.entity.Post;
import com.example.HonBam.userapi.entity.User;
import lombok.*;

@Getter @Setter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequestDTO {
    private String comment;
    private String postId;

    public Comment toEntity(User user, Post post) {
        return Comment.builder()
                .writer(user.getUserName())
                .comment(this.comment)
                .post(post)
                .userId(user.getId())
                .build();
    }
}
