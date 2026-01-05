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
public class FollowId implements Serializable {

    @Column(name = "follower_id", length = 36, nullable = false)
    private String followerId;

    @Column(name = "following_id", length = 36, nullable = false)
    private String followingId;
}
