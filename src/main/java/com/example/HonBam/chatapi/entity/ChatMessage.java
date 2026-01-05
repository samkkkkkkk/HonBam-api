package com.example.HonBam.chatapi.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
