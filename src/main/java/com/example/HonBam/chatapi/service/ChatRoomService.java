package com.example.HonBam.chatapi.service;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomListResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.dto.response.OpenChatRoomResponseDTO;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.exception.ChatRoomAccessException;
import com.example.HonBam.exception.ChatRoomNotFoundException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public ChatRoomResponse createRoom(String requesterId, CreateRoomRequest dto) {

        boolean isDirect = (dto.getParticipantIds() != null && dto.getParticipantIds().size() == 1);

        ChatRoom room = ChatRoom.builder()
                .ownerId(requesterId)
                .name(isDirect ? "1:1 Chat" : dto.getName())
                .isDirect(isDirect)
                .isOpen(dto.isOpen())
                .allowJoinAll(dto.isOpen() && dto.isAllowJoinAll())
                .build();

        chatRoomRepository.save(room);

        // 생성자 추가
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("요청자를 찾을 수 없습니다."));
        chatRoomUserRepository.save(ChatRoomUser.builder()
                .room(room)
                .user(requester)
                .build());

        // 참여자들 추가
        if (dto.getParticipantIds() != null) {
            for (String targetUserId : dto.getParticipantIds()) {
                User target = userRepository.findById(targetUserId)
                        .orElseThrow(() -> new UserNotFoundException("대상자가 없습니다: " + targetUserId));

                chatRoomUserRepository.save(ChatRoomUser.builder()
                        .room(room)
                        .user(target)
                        .build());
            }

            // direct -> group 자동 변환
            long count = chatRoomUserRepository.countByRoom(room);
            if (count >= 3 && room.isDirect()) {
                room.setDirect(false);
                room.setName(dto.getName());
                chatRoomRepository.save(room);
            }
            
        }

        return ChatRoomResponse.builder()
                .roomId(room.getRoomUuid())
                .name(room.getName())
                .ownerId(room.getOwnerId())
                .isDirect(isDirect)
                .isOpen(room.isOpen())
                .createdAt(room.getCreatedAt())
                .allowJoinAll(room.isAllowJoinAll())
                .build();

    }

    // 기존 채팅방에 유저 초대
    @Transactional
    public void inviteUser(String roomUuid, String requesterId, List<String> targetUserIds) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방 없음"));

        // 방장이나 참여자만 초대 가능
        boolean isParticipant = chatRoomUserRepository.findByRoomAndUser_Id(room, requesterId).isPresent();
        if (!isParticipant) {
            throw new ChatRoomAccessException("참여자만 초대 가능");
        }

        for (String targetUserId : targetUserIds) {
            // 대상 존재 여부 확인
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new UserNotFoundException("대상자 없음: " + targetUserId));

            // 이미 참여중인지 확인
            if (chatRoomUserRepository.findByRoomAndUser(room, target).isEmpty()) {

                chatRoomUserRepository.save(ChatRoomUser.builder()
                        .room(room)
                        .user(target)
                        .build());
            }
        }

        // 3명 이상이면 direct -> group
        long count = chatRoomUserRepository.countByRoom(room);
        if (count >= 3 && room.isDirect()) {
            room.setDirect(false);
            chatRoomRepository.save(room);
        }


    }
    
    // 내가 참여중인 방 리스트
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

    // 오픈 채팅방 리스트
    public List<OpenChatRoomResponseDTO> findOpenRooms(String keyword) {

        List<ChatRoom> rooms;

        if (keyword != null && !keyword.isBlank()) {
            rooms = chatRoomRepository.searchOpenRooms(keyword);
        } else {
            rooms = chatRoomRepository.findByIsOpenTrue();
        }
        return rooms.stream()
                .map(r -> OpenChatRoomResponseDTO.builder()
                        .roonId(r.getRoomUuid())
                        .name(r.getName())
                        .participantCont(r.getParticipants().size())
                        .lastMessageTime(r.getLastMessageTime())
                        .build())
                .collect(Collectors.toList());
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

    @Transactional
    public void joinOpenRoom(String roomUuid, String userId) {
        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new ChatRoomNotFoundException("채팅방이 없음"));
        if (!room.isOpen() || !room.isAllowJoinAll()) {
            throw new ChatRoomAccessException("이 방은 자유입장이 불가능합니다. 초대가 필요합니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자 없음"));

        if (chatRoomUserRepository.findByRoomAndUser(room, user).isPresent()) {
            return; // 이미 참여 중이면 무시
        }

        // 참여 추가
        chatRoomUserRepository.save(ChatRoomUser.builder()
                .room(room)
                .user(user)
                .build());

        // direct + group 자동 전환
        long count = chatRoomUserRepository.countByRoom(room);
        if (count >= 3 && room.isDirect()) {
            room.setDirect(false);
            chatRoomRepository.save(room);
        }

    }
    
    
}
