package com.example.HonBam.chatapi.servi;

import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;


    public void saveMessage(ChatMessageDTO messageDto) {
        // 채팅방과 사용자 객체 조화
        ChatRoom chatRoom = chatRoomRepository.findById(Long.valueOf(messageDto.getRoomId()))
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));

        User sender = userRepository.findById(messageDto.getSender())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // ChatMessage 객체 생성
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(messageDto.getContent())
                .timestamp(LocalDateTime.now())
                .build();

        // 메시지 저장
        chatMessageRepository.save(message);

    }
}
