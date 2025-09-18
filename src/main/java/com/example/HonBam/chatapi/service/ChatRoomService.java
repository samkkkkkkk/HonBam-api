package com.example.HonBam.chatapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.dto.response.ChatRoomUserResponse;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    public ChatRoom findByRoomUuid(String uuid) {
        return chatRoomRepository.findByRoomUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + uuid));
    }

    public ChatRoomResponse createRoom(CreateRoomRequest request, TokenUserInfo userInfo) {
        // 1. 방장 유저 조회
        User owner = userRepository.findById(userInfo.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 채팅방 생성
        ChatRoom room = ChatRoom.builder()
                .roomUuid(UUID.randomUUID().toString())
                .name(request.getName())
                .ownerId(owner.getId())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(room);

        // 3. 방장을 참여자 매핑 테이블에 추가
        ChatRoomUser ownerJoin = ChatRoomUser.builder()
                .room(savedRoom)
                .user(owner)
                .build();
        chatRoomUserRepository.save(ownerJoin);

        // 4. 응답 DTO 반환
        return ChatRoomResponse.builder()
                .roomId(savedRoom.getRoomUuid())
                .name(savedRoom.getName())
                .ownerId(savedRoom.getOwnerId())
                .createdAt(savedRoom.getCreatedAt())
                .build();
    }
}
