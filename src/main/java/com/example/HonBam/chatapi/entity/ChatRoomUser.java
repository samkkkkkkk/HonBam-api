package com.example.HonBam.chatapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString(exclude = {"chatRoom", "user"})
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatUserId;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "id", nullable = false)
    private User user;

    private LocalDateTime joinedAt;

    public ChatRoomUser(User user, ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        this.user = user;
    }
}
