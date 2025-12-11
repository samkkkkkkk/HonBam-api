package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByRoomUuid(String roomUuid);

    // 공개 채팅방 전체 조회
    List<ChatRoom> findByOpenTrue();

    // 공개 채팅방 + 키워드 검색
    @Query("SELECT r FROM ChatRoom r WHERE r.open = true AND r.customName LIKE CONCAT('%', :keyword, '%')")
    List<ChatRoom> searchOpenRooms(@Param("keyword") String keyword);

    @Query("SELECT r FROM ChatRoom r " +
            "JOIN r.participants u1 " +
            "JOIN r.participants u2 " +
            "WHERE u1.user.id = :firstId " +
            "AND u2.user.id = :secondId " +
            "AND r.direct = true")
    Optional<ChatRoom> findDirectRoom(@Param("firstId") String firstId,
                                      @Param("secondId") String secondId);

    @Modifying
    @Query("UPDATE ChatRoom c " +
            "SET c.lastMessage = :msg, " +
            "c.lastMessageId = :messageId," +
            "c.lastMessageTime = :time " +
            "WHERE c.id = :roomId")
    int updateLastMessage(@Param("roomId") Long roomId,
                          @Param("msg") String msg,
                          @Param("time") LocalDateTime time,
                          @Param("messageId") Long messageId);

}
