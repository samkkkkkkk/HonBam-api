package com.example.HonBam;

import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.userapi.entity.Role;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
public class ChatMessagePerformanceTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void bulkInsertTestData() {
        // 유저 10명만 삽입 (중복 방지: 이미 있으면 패스)
        IntStream.range(0, 10).forEach(i -> {
            String userId = "user-" + i;
            if (!userRepository.existsById(userId)) {
                User user = User.builder()
                        .id(userId)
                        .nickname("사용자 " + i)
                        .email("user12" + i + "@test.com")
                        .role(Role.COMMON)
                        .password("1")
                        .userName("김" + i)
                        .build();
                userRepository.save(user);
            }
        });

        // 테스트용 채팅방 생성
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .roomUuid("test-room-uuid12")
                .customName("테스트 채팅방")
                .ownerId("user-0")   // 실제 존재하는 유저 ID
                .direct(false)
                .open(false)
                .allowJoinAll(false)
                .build());

        // 메시지 1000개 생성 (sender는 user-0 ~ user-9 순환)
        List<ChatMessage> messages = IntStream.range(0, 1000)
                .mapToObj(i -> ChatMessage.builder()
                        .room(room)
                        .senderId("user-" + (i % 10))   // 10명만 순환
                        .senderName("사용자 " + (i % 10))
                        .content("테스트 메시지 " + i)
                        .build())
                .collect(Collectors.toList());

        StopWatch sw = new StopWatch();
        sw.start();
        chatMessageRepository.saveAll(messages);
        sw.stop();

        System.out.println("메시지 1000개 삽입 걸린 시간(ms): " + sw.getTotalTimeMillis());

    }

}
