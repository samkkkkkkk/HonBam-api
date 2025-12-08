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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

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
    @MockBean
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

    @Test
    void concurrent_message_saving() throws Exception {

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;

            executor.submit(() -> {
                try {
                    ChatRoom chatRoom = chatRoomRepository.findByRoomUuid(room.getRoomUuid()).get();
                    ChatMessageRequest req = ChatMessageRequest.builder()
                            .roomUuid(chatRoom.getRoomUuid())
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

        long count = chatMessageRepository.countByRoomId(room.getId());
        assertThat(count).isEqualTo(threadCount);

        ChatRoom updated = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(updated.getLastMessageId()).isPositive();
        System.out.println("최종 lastMessageId = " + updated.getLastMessageId());
    }

    @Test
    void concurrent_message_inserts_only() throws Exception {

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;

            executor.submit(() -> {
                try {
                    ChatRoom chatRoom = chatRoomRepository.findByRoomUuid(room.getRoomUuid()).get();
                    ChatMessage message = ChatMessage.builder()
                            .room(chatRoom)
                            .senderId("userA")
                            .senderName("홍길동")
                            .messageType(MessageType.TEXT)
                            .content("msg-" + index)
                            .build();

                    chatMessageRepository.save(message);

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long count = chatMessageRepository.countByRoomId(room.getId());
        assertThat(count).isEqualTo(threadCount);
    }

}
