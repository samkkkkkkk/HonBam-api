package com.example.HonBam.chatapi.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@ToString
@Table(name = "chat_room")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 PK (Auto Increment)

    @Column(name = "room_uuid", nullable = false, unique = true, length = 36)
    private String roomUuid; // 외부 노출용 UUID

    @Column(name = "custom_name", nullable = true)
    private String customName; // 방 이름

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId; // 방 생성자 (hb_user.id)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "is_direct", nullable = false)
    private boolean direct;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "allow_join_all", nullable = false)
    private boolean allowJoinAll;

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


    // group 메시지 전환 메서드
    public void convertToGroup(long userCount, String newName) {
        if (userCount >= 3 && this.isDirect()) {
            this.direct = false;

            if (newName != null && !newName.isBlank()) {
                this.customName = newName;
            }
        }
    }
}
