package com.example.HonBam.Chat.room;

import com.example.HonBam.chatapi.component.ChatEventBroadcaster;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.MessageType;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.service.ChatMessageService;
import com.example.HonBam.chatapi.service.ChatRoomService;
import com.example.HonBam.upload.service.PresignedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ChatRoomConcurrencyTest {

    @Autowired
    ChatMessageService chatMessageService;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    ChatMessageRepository chatMessageRepository;

    @MockBean
    ChatEventBroadcaster broadcaster;
    @MockBean
    PresignedUrlService presignedUrlService;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    // ChatRoomService가 ChatMessageService 생성자에서 필요하기 때문에 Mock 제공
    @Autowired
    ChatRoomService chatRoomService;

    private ChatRoom room;

    @BeforeEach
    void setUp() {
        room = ChatRoom.builder()
                .ownerId("owner1")
                .customName("test room")
                .direct(false)
                .open(false)
                .allowJoinAll(false)
                .build();

        chatRoomRepository.save(room);
    }

//    @Test
//    void concurrent_message_saving() throws Exception {
//
//        int threadCount = 100;
//        ExecutorService executor = Executors.newFixedThreadPool(20);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            int index = i;
//
//            executor.submit(() -> {
//                try {
//                    ChatRoom chatRoom = chatRoomRepository.findByRoomUuid(room.getRoomUuid()).get();
//                    ChatMessageRequest req = ChatMessageRequest.builder()
//                            .roomUuid(chatRoom.getRoomUuid())
//                            .messageType(MessageType.TEXT)
//                            .content("msg-" + index)
//                            .build();
//
//                    chatMessageService.saveMessage(req, "userA", "홍길동");
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//
//        long count = chatMessageRepository.countByRoomId(room.getId());
//        assertThat(count).isEqualTo(threadCount);
//
//        ChatRoom updated = chatRoomRepository.findById(room.getId()).orElseThrow();
//        assertThat(updated.getLastMessageId()).isPositive();
//        System.out.println("최종 lastMessageId = " + updated.getLastMessageId());
//    }
//
//    @Test
//    void concurrent_message_inserts_only() throws Exception {
//
//        int threadCount = 100;
//        ExecutorService executor = Executors.newFixedThreadPool(20);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            int index = i;
//
//            executor.submit(() -> {
//                try {
//                    ChatRoom chatRoom = chatRoomRepository.findByRoomUuid(room.getRoomUuid()).get();
//                    ChatMessage message = ChatMessage.builder()
//                            .room(chatRoom)
//                            .senderId("userA")
//                            .senderName("홍길동")
//                            .messageType(MessageType.TEXT)
//                            .content("msg-" + index)
//                            .build();
//
//                    chatMessageRepository.save(message);
//
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//
//        long count = chatMessageRepository.countByRoomId(room.getId());
//        assertThat(count).isEqualTo(threadCount);
//    }

    @Test
    void last_message_should_point_to_latest_message() throws Exception {

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;

            executor.submit(() -> {
                try {
                    ChatMessageRequest req = ChatMessageRequest.builder()
                            .roomUuid(room.getRoomUuid())
                            .messageType(MessageType.TEXT)
                            .content("msg-" + index)
                            .build();

                    chatMessageService.saveMessage(req, "userA", "홍길동");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();


        long start = System.currentTimeMillis();
        Long maxMessageId = 0L;
        ChatRoom updatedRoom = null;

        while (System.currentTimeMillis() - start < 10000) { // 최대 10초 대기
            updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

            // 실제 저장된 가장 마지막 메시지 ID 조회
            maxMessageId = chatMessageRepository.findTopByRoomIdOrderByIdDesc(room.getId())
                    .map(ChatMessage::getId)
                    .orElse(0L);

            // DB 업데이트가 끝까지 따라잡았는지 확인
            if (updatedRoom.getLastMessageId() != null && updatedRoom.getLastMessageId().equals(maxMessageId)) {
                break;
            }
            Thread.sleep(500); // 0.5초마다 확인
        }

        // 메시지  저장 검증
        long count = chatMessageRepository.countByRoomId(room.getId());
        assertThat(count).isEqualTo(threadCount);

        System.out.println("lastMessageId = " + updatedRoom.getLastMessageId());
        System.out.println("expected maxMessageId = " + maxMessageId);

        // 2) 검증
        assertThat(updatedRoom.getLastMessageId()).isNotNull();
        assertThat(updatedRoom.getLastMessageId()).isEqualTo(maxMessageId);
    }
}
