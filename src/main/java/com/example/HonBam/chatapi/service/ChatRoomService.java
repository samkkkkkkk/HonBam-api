package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.ChatRoomResponseDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatNotificationService chatNotificationService; // Inject ChatNotificationService
    private final ChatMessageRepository chatMessageRepository;



    // 사용자가 채팅방에 속해 있는지 확인
    public boolean isUserInChatRoom(String roomId, String sender) {
        try {
            Long roomIdLong = Long.parseLong(roomId); // String -> Long 변환
            // ChatRoomUserRepository에 해당 메소드가 정의되어 있다고 가정
            // 예: boolean existsByChatRoom_RoomIdAndUser_Id(Long roomId, String userId);
            return chatRoomUserRepository.existsByChatRoom_RoomIdAndUser_Id(roomIdLong, sender);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 roomId 형식입니다: " + roomId, e);
        }    }

    @Transactional
    public ChatRoomResponseDTO createChatRoom(String name, List<String> userIds) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomName(name);
        // 채팅방 생성 시 아바타 설정 (determineAvatar 로직이 구현되었다고 가정)
        // chatRoom.setAvatarUrl(determineAvatarForNewRoom(userIds)); // 예시: 새 방 아바타 결정 로직
        // 또는 기본 아바타 설정
        chatRoom.setAvatarUrl("/images/avatar_group.png"); // 임시 기본값
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 사용자를 채팅방에 추가
//        for (String userId : userIds) {
//            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));
//            ChatRoomUser chatRoomUser = new ChatRoomUser(user, chatRoom);
//            chatRoomUserRepository.save(chatRoomUser);
//
//        }

        userIds.stream().map(userid -> userRepository.findById(userid).orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다.")))
                .forEach(user -> {
                    ChatRoomUser chatroomUser = new ChatRoomUser(user, savedChatRoom);
                    chatRoomUserRepository.save(chatroomUser);
                });

        // DTO 생성 (프론트엔드에서 필요한 정보 포함)
        ChatRoomResponseDTO roomResponse = new ChatRoomResponseDTO(
                String.valueOf(savedChatRoom.getRoomId()), // ID를 String으로 변환 (프론트엔드와 일치 확인)
                savedChatRoom.getRoomName(),
                userIds, // ChatRoomResponseDTO에 이 필드가 없다면 제거하거나 다른 방식으로 처리
                "채팅방이 생성되었습니다.", // 초기 메시지
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), // 현재 시간
                savedChatRoom.getAvatarUrl(), // 저장된 채팅방의 아바타 사용
                0 // 초기 안 읽은 메시지 수
        );

        // 새로운 채팅방 생성 알림 발송
        // userIds는 생성자와 초대된 사용자를 모두 포함해야 합니다.
        // CreateChatRoomModal에서 memberIdsIncludingCreator를 보내므로, 이 userIds를 그대로 사용합니다.
        chatNotificationService.notifyNewRoomCreated(roomResponse, userIds);
        return roomResponse;
    }


    @Transactional
    public void inviteUser(Long chatRoomId, String userId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다: " + chatRoomId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다: " + userId));

        // 중복 초대 방지
        if (chatRoomUserRepository.existsByChatRoom_RoomIdAndUser_Id(chatRoomId, userId)) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        // 새로운 관계 추가
        ChatRoomUser chatRoomUser = new ChatRoomUser(user, chatRoom);
        chatRoomUserRepository.save(chatRoomUser);

    }

    public List<ChatRoomResponseDTO> findMyChatRooms(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 사용자가 참여하고 있는 채팅방 목록 조회
        // ChatRoomRepository에 findByChatRoomUsers_User_Id 와 같은 메소드가 정의되어 있다고 가정
        List<ChatRoom> rooms = chatRoomRepository.findByParticipants_User_Id(userId);

        return rooms.stream().map(room -> {
            // 각 방의 마지막 메시지 조회
            ChatMessage lastMessageEntity = chatMessageRepository.findTopByChatRoom_RoomIdOrderByMessageTimeDesc(room.getRoomId()).orElse(null);

            String lastMessageContent = "아직 메시지가 없습니다.";
            String timestamp = "";
            if (lastMessageEntity != null) {
                lastMessageContent = lastMessageEntity.getMessage();
                timestamp = lastMessageEntity.getMessageTime().format(DateTimeFormatter.ofPattern("a h:mm"));
            }

            // TODO: unreadCount 계산 로직 추가 (현재는 0으로 고정)
            // 이 부분은 각 사용자별로 마지막으로 읽은 메시지 정보를 저장하고 비교해야 하므로 복잡합니다.
            // 간단하게는 클라이언트에서 새 메시지 알림을 받을 때 자체적으로 카운트하고,
            // 방에 입장할 때 0으로 리셋하는 방식을 유지할 수 있습니다.
            // 서버에서 정확한 unreadCount를 제공하려면 추가적인 read_status 테이블/로직이 필요합니다.
            int unreadCount = 0;

            return ChatRoomResponseDTO.builder()
                    .id(String.valueOf(room.getRoomId()))
                    .name(room.getRoomName())
                    .lastMessage(lastMessageContent)
                    .timestamp(timestamp)
                    .avatar(room.getAvatarUrl() != null ? room.getAvatarUrl() : determineAvatar(room))
                    .unreadCount(unreadCount)
                    .build();
        }).collect(Collectors.toList());
    }

    // 채팅방 아바타 결정 로직 (예시)
    private String determineAvatar(ChatRoom room) {
        // 1:1 채팅이고 상대방 아바타가 있다면 그것을 사용, 그룹 채팅은 기본 아바타 등
        // 이 로직은 프로젝트의 요구사항에 따라 상세하게 구현해야 합니다.
        return "/images/avatar_group.png"; // 기본값
    }
    
//     새 채팅방 생성 시 아바타 결정 로직 (예시)
     private String determineAvatarForNewRoom(List<String> userIds) {
         if (userIds.size() == 2) return "/images/avatar_user_default.png"; // 1:1 채팅 기본 아바타
         return "/images/avatar_group.png"; // 그룹 채팅 기본 아바타
     }

}
