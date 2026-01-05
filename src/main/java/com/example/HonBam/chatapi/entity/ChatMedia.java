package com.example.HonBam.chatapi.entity;

import com.example.HonBam.upload.entity.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_media")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
