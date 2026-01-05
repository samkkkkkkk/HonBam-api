package com.example.HonBam.Chat.room;

import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.entity.MessageType;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatRoomRepository;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.chatapi.service.ChatMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class ChatPerformanceTest {

    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatRoomUserRepository chatRoomUserRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired private
    EntityManager em;


    @TestConfiguration
    static class TestConfig {


        @Bean(name = "chatTaskExecutor")
        public TaskExecutor asyncTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(20);
            executor.setMaxPoolSize(100);
            executor.setQueueCapacity(200);
            executor.initialize();
            return executor;
        }
    }
    @Test
    @DisplayName("채팅 전송 성능 및 정합성 테스트 (동시 요청 100개)")
    void measureChatThroughput() throws InterruptedException {
        // Given
        int requestCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);

        // 테스트 요청 객체 미리 생성
        ChatMessageRequest request = ChatMessageRequest.builder()
                .roomUuid("14fd7d4a-5664-4215-8dc8-727839694cab")
                .content("테스트 메시지입니다.")
                .messageType(MessageType.TEXT)
                .build();


        for(int i=0; i<5; i++) {
            try {
                chatMessageService.saveMessage(request, "4028fbe89b60310d019b603353dd0000", "홍길동");
            } catch (Exception e) {}
        }

        System.out.println("=== 성능 테스트 시작 ===");

        // When
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    chatMessageService.saveMessage(request, "4028fbe89b60310d019b603353dd0000", "홍길동");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 요청 처리가 끝날 때까지 대기 (최대 30초)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Then 1: 성능 지표 출력
        long totalTime = endTime - startTime;
        System.out.println("=========================================");
        System.out.println("총 요청 수: " + requestCount);
        System.out.println("총 소요 시간: " + totalTime + "ms");
        System.out.println("초당 처리량(TPS): " + String.format("%.2f", (requestCount / (totalTime / 1000.0))));
        System.out.println("평균 응답 시간: " + String.format("%.2f", (totalTime / (double) requestCount)) + "ms");
        System.out.println("=========================================");

        if(!completed) {
            System.out.println("일부 요청이 시간 내에 완료되지 않았습니다.");
        }

        // Then 2: 데이터 정합성 검증 실행
        verifyDataConsistency(requestCount + 5); // Warm-up 5개 포함
    }

    private void verifyDataConsistency(int expectedTotalCount) {
        // 영속성 컨텍스트 초기화 (DB의 최신 상태를 읽기)
        em.clear();

        // 1. 메시지 총 개수 검증
        long savedCount = chatMessageRepository.countByRoomId(1L);
        assertThat(savedCount).isEqualTo(expectedTotalCount);

        // 2. 채팅방의 LastMessageId 갱신 여부 검증
        ChatRoom updatedRoom = chatRoomRepository.findById(1L).orElseThrow();

        // 실제 저장된 가장 최신 메시지 조회
        ChatMessage realLastMessage = chatMessageRepository.findTopByRoomOrderByTimestampDesc(updatedRoom)
                .orElseThrow(() -> new RuntimeException("메시지가 저장되지 않음"));

        System.out.println("[정합성 검증]");
        System.out.println("Expect(DB Real Max ID): " + realLastMessage.getId());
        System.out.println("Actual(Room Last ID)  : " + updatedRoom.getLastMessageId());

        // Room 엔티티의 포인터가 실제 메시지 ID와 일치해야 함
        assertThat(updatedRoom.getLastMessageId()).isNotNull();
        assertThat(updatedRoom.getLastMessageId()).isEqualTo(realLastMessage.getId());

        System.out.println("데이터 정합성 검증 통과");
    }
}