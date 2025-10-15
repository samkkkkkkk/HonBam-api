package com.example.HonBam.chatapi.scheduler;

import ch.qos.logback.core.util.Loader;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.entity.ChatRead;
import com.example.HonBam.chatapi.entity.ChatReadId;
import com.example.HonBam.chatapi.repository.ChatMessageRepository;
import com.example.HonBam.chatapi.repository.ChatReadRepository;
import com.example.HonBam.userapi.repository.UserRepository;
import com.example.HonBam.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatReadFlushScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatReadRepository chatReadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60000)
    public void flushChatReads() {
        Set<String> keys = redisTemplate.keys("chat:read:temp:*");
        if(keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length < 4) continue;

            Long roomId = Long.parseLong(parts[2]);
            String userId = parts[3];

            Set<Object> messageIds = redisTemplate.opsForSet().members(key);
            if(messageIds == null || messageIds.isEmpty()) continue;

            for (Object msgId : messageIds) {
                Long messageId = Long.parseLong(msgId.toString());
                ChatReadId id = new ChatReadId(messageId, userId);

                if (!chatReadRepository.existsById(id)) {
                    ChatMessage message = chatMessageRepository.findById(messageId).orElse(null);
                    if (message != null) {
                        ChatRead read = ChatRead.builder()
                                .id(id)
                                .message(message)
                                .user(userRepository.getReferenceById(userId))
                                .readAt(LocalDateTime.now())
                                .build();
                        chatReadRepository.save(read);
                    }

                    redisTemplate.delete(key);
                    log.info("[FLUSH] chat reads flushed from {}", key);

                }
            }
        }
    }

}
