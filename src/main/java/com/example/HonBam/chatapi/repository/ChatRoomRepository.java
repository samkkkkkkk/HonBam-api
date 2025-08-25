package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT c FROM ChatRoom c ORDER BY c.lastMessageTime DESC")
    List<ChatRoom> findAllOrderByLastMessageTime();

    // 채팅방에 특정 사용자가 포함되어 있는지 확인하는 쿼리 메서드
    boolean existsByRoomIdAndParticipants_ChatUserId(Long roomId, Long chatUserId);

    List<ChatRoom> findByParticipants_User_Id(String Id);
}
