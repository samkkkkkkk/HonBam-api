package com.example.HonBam.chatapi.api;

// import com.example.HonBam.chatapi.component.RedisPublisher; // RabbitMQ 사용으로 RedisPublisher 제거

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.repository.ChatRoomUserRepository;
import com.example.HonBam.chatapi.service.ChatMessageService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/chat/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository; // UserRepository 주입

    // 채팅방 기존 메시지 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessages(
            @RequestParam("roomUuid") String roomUuid,
            Principal principal
    ) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            TokenUserInfo user = (TokenUserInfo)
                    ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            log.info("메시지 목록 요청: roomUuid={}, userId{}", roomUuid, user.getUserId());
        }

        List<ChatMessageResponseDTO> messages = chatMessageService.getMessagesByRoom(roomUuid);
        return ResponseEntity.ok(messages);

    }

    // 메시지 목록 페이징 처리
    @GetMapping("/page")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessagesPage(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<ChatMessageResponseDTO> messages = chatMessageService.getMessageNative(roomId, page, size);
        return ResponseEntity.ok(messages);

    }

    @GetMapping("/cursor")
    public ResponseEntity<List<ChatMessageResponseDTO>> getMessagesCursor(
            @RequestParam("roomUuid") String roomUuid,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(defaultValue = "30") int size,
            Principal principal
    ) {
        LocalDateTime cursorTime = null;
        if (cursor != null && !cursor.isBlank()) {
            cursorTime = LocalDateTime.parse(cursor);
        }

        List<ChatMessageResponseDTO> messages = chatMessageService.getMessagesCursor(roomUuid, cursorTime, size);
        return ResponseEntity.ok(messages);
    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageRequest request,
                            Principal principal) {

        if (principal instanceof UsernamePasswordAuthenticationToken) {
            TokenUserInfo user = (TokenUserInfo) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            log.info("유저아이디: {}", user.getUserId());

            User sender = userRepository.findById(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("메시지 전송 사용자를 찾을 수 없습니다."));

            log.info("메시지 내용: {}", request.getContent());

            ChatMessageResponseDTO response = chatMessageService.saveMessage(
                    request,
                    sender.getId(),
                    sender.getNickname()
            );

            Map<String, Object> payload = Map.of(
                    "type", "MESSAGE",
                    "body", response
            );

            messagingTemplate.convertAndSend("/topic/chat.room." + request.getRoomUuid(), payload);
        } else {
            log.warn("메시지 전송: 인증된 사용자 아님");
        }
    }


}