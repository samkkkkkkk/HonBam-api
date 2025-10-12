package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    List<ChatMessage> findByRoomId(Long roomId);

    // 내가 읽지 않은 메시지 개수
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.room.id = :roomId AND m.id > :messageId")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("messageId") Long messageId);

    // 전체 메시지 개수
    long countByRoomId(Long roomId);

    // JPA Pageable 방식
    Page<ChatMessage> findByRoomIdOrderByTimestampDesc(Long roomId, Pageable pageable);

    // Native Query 방식
    @Query(value = "SELECT * FROM chat_message " +
            "WHERE room_id = :roomId " +
            "ORDER BY timestamp DESC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<ChatMessage> findMessageWithPaging(
            @Param("roomId") Long roomId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    Optional<ChatMessage> findTopByRoomOrderByTimestampDesc(ChatRoom room);

    @Query(value = "SELECT * FROM chat_message " +
            "WHERE room_id = :roomId " +
            "AND (:cursorTime IS NULL OR timestamp < :cursorTime) " +
            "ORDER BY timestamp DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findMessagesBefore(@Param("roomId") Long roomId,
                                         @Param("cursorTime") LocalDateTime cursorTime,
                                         @Param("limit") int limit);

}
