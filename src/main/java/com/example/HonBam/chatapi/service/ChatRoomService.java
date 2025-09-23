package com.example.HonBam.chatapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomListResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserUtil userUtil;

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

    public List<ChatRoomListResponseDTO> roomList(TokenUserInfo userInfo) {
        List<ChatRoomUser> cruList = chatRoomUserRepository.findUsersByUserId(userInfo.getUserId());

        if (cruList.isEmpty()) {
            return Collections.emptyList();
        }

        return cruList.stream()
                .map(cru -> {
                    ChatRoom r = cru.getRoom();
                    long unread = chatMessageRepository.countUnreadMessages(r.getId(), cru.getLastReadTime());

                    return ChatRoomListResponseDTO.builder()
                            .roomId(r.getRoomUuid())
                            .name(r.getName())
                            .lastMessage(r.getLastMessage())
                            .lastMessageTime(r.getLastMessageTime())
                            .unReadCount((int) unread)
                            .build();
                }).collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomResponse startDirectChat(String requesterId, String targetUserId) {
        // 1. 기존 방이 있는지 확인
        Optional<ChatRoom> existingRoom = chatRoomUserRepository.findDirectChatRoom(requesterId, targetUserId);
        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();
            return ChatRoomResponse.builder()
                    .roomId(room.getRoomUuid())
                    .name(room.getName())
                    .ownerId(room.getOwnerId())
                    .createdAt(room.getCreatedAt())
                    .build();
        }

        // 2. 새로운 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .name("1:1 Chat")
                .ownerId(requesterId)
                .build();
        chatRoomRepository.save(newRoom);

        // 3. 참여자 추가
        User requester = userRepository.findById(requesterId).orElseThrow();
        User target = userRepository.findById(targetUserId).orElseThrow();

        chatRoomUserRepository.save(ChatRoomUser.builder().room(newRoom).user(requester).build());
        chatRoomUserRepository.save(ChatRoomUser.builder().room(newRoom).user(target).build());

        return ChatRoomResponse.builder()
                .roomId(newRoom.getRoomUuid())
                .ownerId(newRoom.getOwnerId())
                .name(newRoom.getName())
                .createdAt(newRoom.getCreatedAt())
                .build();
    }
}
