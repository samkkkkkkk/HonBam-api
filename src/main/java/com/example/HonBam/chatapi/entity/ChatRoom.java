package com.example.HonBam.chatapi.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_room")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 PK (Auto Increment)

    @Column(name = "room_uuid", nullable = false, unique = true, length = 36)
    private String roomUuid; // 외부 노출용 UUID

    @Column(nullable = false)
    private String name; // 방 이름

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId; // 방 생성자 (hb_user.id)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatRoomUser> participants = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.roomUuid == null) {
            this.roomUuid = UUID.randomUUID().toString();
        }
    }
}
