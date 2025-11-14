package com.example.HonBam.snsapi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "sns_post_like")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
