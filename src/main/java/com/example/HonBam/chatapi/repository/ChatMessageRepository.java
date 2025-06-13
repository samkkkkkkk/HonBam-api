package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomRoomIdOrderByMessageTimeAsc(Long roomId);
    Optional<ChatMessage> findTopByChatRoom_RoomIdOrderByMessageTimeDesc(Long chatRoomId); // Corrected method

}
