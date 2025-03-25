package com.example.HonBam.chatapi.service;

import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.ChatRoomUser;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;


    // 사용자가 채팅방에 속해 있는지 확인
    public boolean isUserInChatRoom(String roomId, String sender) {
        return chatRoomRepository.existsByIdAndParticipants_Id(roomId, sender);
    }

    @Transactional
    public ChatRoom createChatRoom(String name, List<String> userIds) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomName(name);
        chatRoomRepository.save(chatRoom);

        // 사용자를 채팅방에 추가
//        for (String userId : userIds) {
//            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));
//            ChatRoomUser chatRoomUser = new ChatRoomUser(user, chatRoom);
//            chatRoomUserRepository.save(chatRoomUser);
//
//        }

        userIds.stream().map(userid -> userRepository.findById(userid).orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다.")))
                .forEach(user -> {
                    ChatRoomUser chatroomUser = new ChatRoomUser(user, chatRoom);
                    chatRoomUserRepository.save(chatroomUser);
                });

        return chatRoom;
    }


    @Transactional
    public void inviteUser(Long chatRoomId, String userId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않느 사용자입니다."));

        // 중복 초대 방지
        if (chatRoomUserRepository.existByChatRoomAndUser(chatRoom, user)) {
            throw new RuntimeException("이미 존재하는 사용자입니다.");
        }

        // 새로운 관계 추가
        ChatRoomUser chatRoomUser = new ChatRoomUser(user, chatRoom);
        chatRoomUserRepository.save(chatRoomUser);

    }
}
