package com.example.HonBam.chatapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.response.ChatRoomUserResponse;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomUserService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    public void joinRoom(String roomUuid, TokenUserInfo userInfo) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        User user = userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        ChatRoomUser join = ChatRoomUser.builder()
                .room(room)
                .user(user)
                .build();

        chatRoomUserRepository.save(join);
    }

    public void leaveRoom(String roomUuid, TokenUserInfo userInfo) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        chatRoomUserRepository.deleteByRoomAndUser_Id(room, userInfo.getUserId());
    }

    public List<ChatRoomUserResponse> getParticipants(String roomUuid) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        return chatRoomUserRepository.findByRoom(room)
                .stream()
                .map(cu -> ChatRoomUserResponse.from(cu.getUser()))
                .collect(Collectors.toList());
    }
}
