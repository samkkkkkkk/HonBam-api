package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
//    boolean existByChatRoomRoomIdAndUserId(ChatRoom chatRoom, User user);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM ChatRoomUser c WHERE c.user.id = :userId AND c.chatRoom.roomId = :roomId")
    boolean isExistByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") String userId);

    boolean existsByChatRoom_RoomIdAndUser_Id(Long chatRoomId, String userId);
}
