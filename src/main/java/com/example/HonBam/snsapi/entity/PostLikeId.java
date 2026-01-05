package com.example.HonBam.snsapi.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostLikeId implements Serializable {

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;
}
