package com.example.HonBam.chatapi.api;

// import com.example.HonBam.chatapi.component.RedisPublisher; // RabbitMQ 사용으로 RedisPublisher 제거

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.ChatMessageRequest;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.service.ChatMessageService;
import com.example.HonBam.userapi.entity.User;
import com.example.HonBam.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository; // UserRepository 주입

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest request,
                            Principal principal) {

        if (principal instanceof UsernamePasswordAuthenticationToken) {
            TokenUserInfo user = (TokenUserInfo) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            log.info("유저아이디: {}", user.getUserId());

            User sender = userRepository.findById(user.getUserId())
                    .orElseThrow(() -> new RuntimeException("메시지 전송 사용자를 찾을 수 없습니다."));

            ChatMessageResponseDTO response = chatMessageService.saveMessage(
                    request,
                    sender.getId(),
                    sender.getNickname()
            );

            messagingTemplate.convertAndSend("/topic/room." + request.getRoomId(), response);
        } else {
            log.warn("메시지 전송: 인증된 사용자 아님");
        }
    }

}