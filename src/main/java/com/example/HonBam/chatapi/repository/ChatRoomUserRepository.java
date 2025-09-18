package com.example.HonBam.chatapi.repository;

import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.userapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
    List<ChatRoomUser> findByRoom(ChatRoom room);
    Optional<ChatRoomUser> findByRoomAndUser(ChatRoom room, User user);
    void deleteByRoomAndUser(ChatRoom room, User user);
    void deleteByRoomAndUser_Id(ChatRoom room, String userId);

}
