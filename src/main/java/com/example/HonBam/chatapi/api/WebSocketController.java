package com.example.HonBam.chatapi.api;

import com.example.HonBam.chatapi.component.RedisPublisher;
import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.entity.ChatMessage;
import com.example.HonBam.chatapi.servi.ChatMessageService;
import com.example.HonBam.chatapi.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final RedisPublisher redisPublisher;
    private final ChatService chatService;
    private final ChatMessageService chatMessageService;


    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO message) {
        redisPublisher.publish("chatroom", message); // Redis에 저장
        chatMessageService.saveMessage(message);

    }

    @GetMapping("/chat/history/{roomId}")
    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
        return chatService.getChatHistory(roomId);
    }

//    @MessageMapping("/message") // 클라이언트에서 /app/message로 보낸 요청 처리
//    @SendTo("/topic/messages") // 결과를 /topic/messages로 브로드 캐스팅
//    public MessageDTO sendMessage(@Payload MessageDTO dto) {
//        log.info("받은 메세지 {}", dto);
//        return dto;
//    }


}
