package com.example.HonBam.snsapi.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sns_follow")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
