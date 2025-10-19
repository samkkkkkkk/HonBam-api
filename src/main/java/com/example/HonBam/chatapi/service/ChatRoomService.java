package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.dto.ChatReadEvent;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomListResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.dto.response.OpenChatRoomResponseDTO;
import com.example.HonBam.chatapi.entity.*;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatReadRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.exception.ChatRoomAccessException;
import com.example.HonBam.exception.ChatRoomNotFoundException;
import com.example.HonBam.exception.ChatRoomValidationException;
import com.example.HonBam.exception.UserNotFoundException;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadRepository chatReadRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserUtil userUtil;

    public ChatRoom findByRoomUuid(String uuid) {
        return chatRoomRepository.findByRoomUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다: " + uuid));
    }

    @Transactional
    public ChatRoomResponse createRoom(String requesterId, CreateRoomRequest dto) {

        if (!dto.isOpen() && (dto.getParticipantIds() == null || dto.getParticipantIds().isEmpty())) {
            throw new ChatRoomValidationException("일반 채팅방을 생성할 때 최소 1명 이상의 참여자가 필요합니다.");
        }

        boolean isDirect = !dto.isOpen() && dto.getParticipantIds() != null && dto.getParticipantIds().size() == 1;

        // 오픈 채팅방은 반드시 이름 지정해야 함
        if (dto.isOpen() && (dto.getName() == null || dto.getName().isBlank())) {
            throw new ChatRoomValidationException("오픈 채팅방은 이름을 필수로 입력해야 합니다.");
        }

        ChatRoom room = ChatRoom.builder()
                .ownerId(requesterId)
                .customName(dto.getName() != null && !dto.getName().isBlank() ? dto.getName() : null)
                .direct(isDirect)
                .open(dto.isOpen())
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
                if (dto.getName() != null && !dto.getName().isBlank()) {
                    room.setCustomName(dto.getName());
                }
                chatRoomRepository.save(room);
            }
            
        }

        return ChatRoomResponse.builder()
                .roomUuid(room.getRoomUuid())
                .name(resolveDisplayName(room, requesterId))
                .ownerId(room.getOwnerId())
                .direct(room.isDirect())
                .open(room.isOpen())
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
    
//    // 내가 참여중인 방 리스트
//    public List<ChatRoomListResponseDTO> roomList(TokenUserInfo userInfo) {
//        List<ChatRoomUser> cruList = chatRoomUserRepository.findUserByUserWithParticipants(userInfo.getUserId());
//
//        if (cruList.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        return cruList.stream()
//                .map(cru -> {
//                    ChatRoom r = cru.getRoom();
//                    long unread = chatMessageRepository.countUnreadMessages(r.getId(), cru.getLastReadTime());
//
//                    return ChatRoomListResponseDTO.builder()
//                            .roomUuid(r.getRoomUuid())
//                            .customName(resolveDisplayName(r, userInfo.getUserId()))
//                            .lastMessage(r.getLastMessage())
//                            .lastMessageTime(r.getLastMessageTime())
//                            .unReadCount(unread)
//                            .open(r.isOpen())
//                            .direct(r.isDirect())
//                            .allowJoinAll(r.isAllowJoinAll())
//                            .build();
//                }).collect(Collectors.toList());
//    }

    // 내가 참여중인 방 리스트
    public List<ChatRoomListResponseDTO> roomList(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<ChatRoomUser> joinedRooms = chatRoomUserRepository.findUserByUserWithParticipants(userId);

        return joinedRooms.stream()
                .map(roomUser -> {
                    ChatRoom room = roomUser.getRoom();

                    // 마지막 메시지
                    ChatMessage lastMessage = chatMessageRepository
                            .findTopByRoomOrderByTimestampDesc(room)
                            .orElse(null);

                    // 읽지 않은 메시지 수
                    Long lastReadMessageId = roomUser.getLastReadMessageId();
                    long unreadCount = (lastReadMessageId != null)
                            ? chatMessageRepository.countUnreadMessages(room.getId(), lastReadMessageId, userId)
                            : chatMessageRepository.countByRoomIdAndSenderIdNot(room.getId(), userId);

                    return ChatRoomListResponseDTO.builder()
                            .roomUuid(room.getRoomUuid())
                            .customName(room.getCustomName())
                            .ownerId(room.getOwnerId())
                            .open(room.isOpen())
                            .direct(room.isDirect())
                            .allowJoinAll(room.isAllowJoinAll())
                            .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                            .lastMessageTime(lastMessage != null ? lastMessage.getTimestamp() : null)
                            .lastMessageId(lastMessage != null ? lastMessage.getId() : null)
                            .lastReadMessageId(lastReadMessageId)
                            .unReadCount(unreadCount)
                            .build();
                })
                .sorted(comparing(
                        (ChatRoomListResponseDTO r) ->
                                Optional.ofNullable(r.getLastMessageTime()).orElse(LocalDateTime.MIN),
                        reverseOrder()))
                .collect(Collectors.toList());
    }

    // 오픈 채팅방 리스트
    public List<OpenChatRoomResponseDTO> findOpenRooms(String keyword) {

        List<ChatRoom> rooms;

        if (keyword != null && !keyword.isBlank()) {
            rooms = chatRoomRepository.searchOpenRooms(keyword);
        } else {
            rooms = chatRoomRepository.findByOpenTrue();
        }
        return rooms.stream()
                .map(r -> OpenChatRoomResponseDTO.builder()
                        .roomUuid(r.getRoomUuid())
                        .name(r.getCustomName())
                        .participantCount(r.getParticipants().size())
                        .lastMessageTime(r.getLastMessageTime())
                        .open(r.isOpen())
                        .allowJoinAll(r.isAllowJoinAll())
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
                    .roomUuid(room.getRoomUuid())
                    .name(room.getCustomName())
                    .ownerId(room.getOwnerId())
                    .createdAt(room.getCreatedAt())
                    .build();
        }

        // 2. 새로운 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .customName("1:1 Chat")
                .ownerId(requesterId)
                .build();
        chatRoomRepository.save(newRoom);

        // 3. 참여자 추가
        User requester = userRepository.findById(requesterId).orElseThrow();
        User target = userRepository.findById(targetUserId).orElseThrow();

        chatRoomUserRepository.save(ChatRoomUser.builder().room(newRoom).user(requester).build());
        chatRoomUserRepository.save(ChatRoomUser.builder().room(newRoom).user(target).build());

        return ChatRoomResponse.builder()
                .roomUuid(newRoom.getRoomUuid())
                .ownerId(newRoom.getOwnerId())
                .name(newRoom.getCustomName())
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

    // 채팅방 이름 가공 메서드
    private String resolveDisplayName(ChatRoom room, String requesterId) {
        // 사용자가 지정한 이름이 있으면 그 이름을 우선 사용
        if (room.getCustomName() != null && !room.getCustomName().isBlank()) {
            return room.getCustomName();
        }

        // 오픈 채팅
        if (room.isOpen()) {
            return "오픈 채팅방";
        }

        // 다리렉트 -> 상대방 이름
        if (room.isDirect()) {
            return room.getParticipants().stream()
                    .map(ChatRoomUser::getUser)
                    .filter(u -> !u.getId().equals(requesterId))
                    .map(User::getNickname)
                    .findFirst()
                    .orElse("1:1 채팅");
        }

        // 그룹 -> 참여자 전체 이름
        List<String> names = room.getParticipants().stream()
                .map(ChatRoomUser::getUser)
                .filter(u -> !u.getId().equals(requesterId))
                .map(User::getNickname)
                .collect(Collectors.toList());

        if (names.size() > 5) {
            return String.join(", ", names.subList(0, 5)) + " " + names.size();
        }

        return String.join(", ", names);
    }


    @Transactional
    public void updateLastMessage(String roomUuid, String userId, Long messageId) {
        markMessageAsRead(roomUuid, userId, messageId);
        log.info("[READ EVENT BROADCAST] -> STOMP /topic/chat.room.{}.read", roomUuid);
    }
 
    
    // 입장 시 전체 읽음 처리
    @Transactional
    public void markAllAsReadOnJoin(String roomUuid, String userId) {
        log.info("[JOIN READ] room={}, user={}", roomUuid, userId);

        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 마지막 메시지 가져오기
        ChatMessage lastMessage = chatMessageRepository
                .findTopByRoomOrderByTimestampDesc(room)
                .orElse(null);

        // 방에 메시지가 없으면 스킵
        if (lastMessage == null) {
            log.info("[JOIN READ] 메시지 없음 -> 스킵");
            return;
        }

        User reader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("읽음 처리 대상자를 찾을 수 없습니다."));

        // 마지막 메시지를 읽은 것으로 표시
        chatRoomUserRepository.updateLastReadMessageIdIfNewer(room.getId(), userId, lastMessage.getId());

        markMessagesAsReadInternal(room, lastMessage.getId(), reader);
    }


    @Transactional
    public void markMessageAsRead(String roomUuid, String userId, Long messageId) {

        ChatRoom room = chatRoomRepository.findByRoomUuid(roomUuid)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));

        if (message.getSenderId().equals(userId)) {
            log.debug("본인 메시지는 읽음 처리가 필요 없습니다. 받은 메시지Id={}", messageId);
            return;
        }

        User reader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("읽음 처리 대상 사용자를 찾을 수 없습니다."));

        // lastReadMessageId 갱신
        chatRoomUserRepository.updateLastReadMessageIdIfNewer(room.getId(), userId, messageId);

        markMessagesAsReadInternal(room, messageId, reader);
    }

    private void markMessagesAsReadInternal(ChatRoom room, Long lastMessageId, User reader) {
        List<Long> messageIds = chatMessageRepository.findIdsByRoomAndIdLessThanEqualAndNotSender(room.getId(), lastMessageId, reader.getId());

        // DB 기록 (소규모 방은 즉시, 대규모는 flush)
        long participantCount = chatRoomUserRepository.countByRoom(room);


        if (participantCount <= 10) {
            for (Long messageId : messageIds) {
                try {
                    ChatReadId id = new ChatReadId(messageId, reader.getId());
                    if (!chatReadRepository.existsById(id)) {
                        ChatRead read = ChatRead.builder()
                                .id(id)
                                .message(chatMessageRepository.getReferenceById(messageId))
                                .user(reader)
                                .build();
                        chatReadRepository.save(read);
                    }
                } catch (EntityNotFoundException e) {
                    log.warn("[READ SKIP] 메시지가 삭제되어 읽음 처리 생략됨: messageID={}", messageId);
                }
            }
        } else {
            String tempKey = "chat:read:temp:" + room.getId() + ":" + reader.getId();
            redisTemplate.opsForSet().add(tempKey, lastMessageId.toString());
            redisTemplate.expire(tempKey, 5, TimeUnit.MINUTES);
        }

        // Redis 갱신
        redisTemplate.opsForHash().put("chat:read:" + room.getId(), reader.getId(), lastMessageId);


        // 읽지 않은 인원 수 계산
        ChatMessage lastMessage = chatMessageRepository.findById(lastMessageId)
                .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없습니다."));
        long unreadCount = chatRoomUserRepository.countUnreadUsersForMessage(
                room.getId(), lastMessageId, lastMessage.getSenderId());

        // 이벤트 DTO 생성
        ChatReadEvent event = new ChatReadEvent(room.getRoomUuid(), lastMessageId, unreadCount, reader.getId());

        // Redis Pub/Sub + stomp 브로드 캐스트
        redisTemplate.convertAndSend("chat:read:event", event);
        messagingTemplate.convertAndSend("/topic/chat.room." + room.getRoomUuid() + ".read", Map.of(
                "type", "READ_UPDATE",
                "body", event
        ));

        // 채팅방 목록 unread 갱신
        long roomUnreadCount = chatMessageRepository.countUnreadMessagesForRoomAndUser(room.getId(), reader.getId());
        messagingTemplate.convertAndSend("/topic/chat.summary." + reader.getId(), Map.of(
                "type", "ROOM_SUMMARY_UPDATE",
                "body", Map.of("roomUuid", room.getRoomUuid(), "unReadCount", roomUnreadCount)
        ));

        log.info("[READ EVENT - INTERNAL] room={}, messageID={}, unread={}", room.getRoomUuid(), lastMessageId, unreadCount);
    }

}
