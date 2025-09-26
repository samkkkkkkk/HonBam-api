package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomUuid(String roomUuid);

    // 공개 채팅방 전체 조회
    List<ChatRoom> findByOpenTrue();

    // 공개 채팅방 + 키워드 검색
    @Query("SELECT r FROM ChatRoom r WHERE r.open = true AND r.name LIKE CONCAT('%', :keyword, '%')")
    List<ChatRoom> searchOpenRooms(@Param("keyword") String keyword);
}
