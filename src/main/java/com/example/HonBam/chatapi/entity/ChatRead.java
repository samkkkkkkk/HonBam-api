package com.example.HonBam.chatapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_read")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRead {

    @EmbeddedId
    private ChatReadId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

}
