package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select count(m) from ChatMessage m where m.room.id = :roomId AND m.timestamp > :lastReadTime")
    int countUnreadMessages(@Param("roomId") long roomId, @Param("lastReadTime") LocalDateTime lastReadTime);
}
