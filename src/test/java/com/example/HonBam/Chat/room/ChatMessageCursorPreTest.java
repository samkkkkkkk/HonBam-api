package com.example.HonBam.Chat.room;

import com.example.HonBam.chatapi.component.ChatEventBroadcaster;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.MessageType;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.service.ChatMessageService;
import com.example.HonBam.upload.service.PresignedUrlService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ChatMessageCursorPreTest {

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

    private ChatRoom room;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();

        room = ChatRoom.builder()
                .ownerId("owner1")
                .customName("load-test-room")
                .direct(false)
                .open(false)
                .allowJoinAll(false)
                .build();
        chatRoomRepository.save(room);
    }

    @Test
    void getMessageCursor_load_test() throws Exception{
        int threadCount = 5;
        int messagesPerThread  = 10;
        int totalMessages = threadCount * messagesPerThread ;

        ExecutorService executor = Executors.newFixedThreadPool(20);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);


        List<Long> latencies = new ArrayList<>(totalMessages);

        StopWatch totalWatch = new StopWatch("saveMessage-load-test");
        totalWatch.start("all-threads");
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    ready.countDown();      // "준비 완료" 표시
                    start.await();          // 모두 준비될 때까지 대기

                    for (int i = 0; i < messagesPerThread; i++) {
                        ChatMessageRequest req = ChatMessageRequest.builder()
                                .roomUuid(room.getRoomUuid())
                                .messageType(MessageType.TEXT)
                                .content("load-msg")
                                .build();

                        long begin = System.nanoTime();
                        chatMessageService.saveMessage(req, "userA", "홍길동");
                        long end = System.nanoTime();
                        long millis = TimeUnit.NANOSECONDS.toMillis(end - begin);

                        synchronized (latencies) {
                            latencies.add(millis);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 기다렸다가 동시에 시작
        ready.await();
        start.countDown();

        // 모든 작업 완료 대기
        done.await();
        totalWatch.stop();
        executor.shutdown();

        long dbCount = chatMessageRepository.countByRoomId(room.getId());
        assertThat(dbCount).isEqualTo(totalMessages);

        long totalMillis = totalWatch.getTotalTimeMillis();
        double avgLatency = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long p95Latency = latencies.stream()
                .sorted()
                .skip((long) (latencies.size() * 0.95))
                .findFirst()
                .orElse(0L);

        System.out.println(totalWatch.prettyPrint());
        System.out.println("totalMessages = " + totalMessages);
        System.out.println("totalMillis   = " + totalMillis);
        System.out.println("avgLatency(ms)= " + avgLatency);
        System.out.println("p95Latency(ms)= " + p95Latency);

        // 임계값은 환경에 맞게 조정 (예시는 대략적인 값)
        assertThat(avgLatency).isLessThan(100);    // 평균 50ms 미만
        assertThat(p95Latency).isLessThan(200);   // 95퍼센타일 100ms 미만
    }
}
