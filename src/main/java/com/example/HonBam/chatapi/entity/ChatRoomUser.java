package com.example.HonBam.chatapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_user")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)  // FK → ChatRoom.id
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // FK → hb_user.id
    private User user;

    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }
}
