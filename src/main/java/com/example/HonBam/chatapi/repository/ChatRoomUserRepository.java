package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.userapi.entity.User;
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
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
    List<ChatRoomUser> findByRoom(ChatRoom room);

    Optional<ChatRoomUser> findByRoomAndUser(ChatRoom room, User user);

    void deleteByRoomAndUser(ChatRoom room, User user);

    void deleteByRoomAndUser_Id(ChatRoom room, String userId);

//    @Query("select cru from ChatRoomUser cru join fetch cru.room r where cru.user.id = :userId")
//    List<ChatRoomUser> findUsersByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT cru FROM ChatRoomUser cru " +
            "JOIN FETCH cru.room r " +
            "LEFT JOIN FETCH r.participants p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE cru.user.id = :userId")
    List<ChatRoomUser> findUserByUserWithParticipants(@Param("userId") String userId);

    @Query("SELECT cru.room FROM ChatRoomUser cru WHERE cru.user.id IN (:userA, :userB) " +
            "GROUP BY cru.room.id  " +
            "HAVING COUNT(DISTINCT cru.user.id) = 2")
    Optional<ChatRoom> findDirectChatRoom(@Param("userA") String userA, @Param("userB") String userB);


    @Query("SELECT cru FROM ChatRoomUser cru WHERE cru.room = :room AND cru.user.id = :userId")
    Optional<ChatRoomUser> findByRoomAndUser_Id(@Param("room") ChatRoom room, @Param("userId") String userId);

    long countByRoom(ChatRoom room);

    @Query("SELECT COUNT(cru) FROM ChatRoomUser cru " +
            "WHERE cru.room.id = :roomId " +
            "AND cru.lastReadTime < :messageTime")
    long countUnreadUsersForMessage(@Param("roomId") Long roomId,
                                    @Param("messageTime") LocalDateTime messageTime);


    @Query(value = "SELECT m.id AS messageId, COUNT(cu.id) AS unreadCount " +
            "FROM chat_message m " +
            "JOIN chat_room_user cu ON cu.room.id = m.room.id " +
            "WHERE m.room.id = :roomId " +
            "AND cu.last_read_time < m.timestamp " +
            "GROUP BY m.id", nativeQuery = true)
    List<Object[]> countUnreadUsersForMessages(@Param("roomId") Long roomId);

}
