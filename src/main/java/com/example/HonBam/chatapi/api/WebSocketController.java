//package com.example.HonBam.chatapi.api;
//
//// import com.example.HonBam.chatapi.component.RedisPublisher; // RabbitMQ 사용으로 RedisPublisher 제거
//import com.example.HonBam.chatapi.dto.ChatMessageDTO;
//import com.example.HonBam.chatapi.entity.ChatMessage;
//import com.example.HonBam.chatapi.servi.ChatMessageService;
//import com.example.HonBam.chatapi.service.ChatService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.Objects;
//
//@RestController
//@RequiredArgsConstructor
//@Slf4j
//public class WebSocketController {
//
//    // private final RedisPublisher redisPublisher; // RabbitMQ 사용으로 제거
//    private final ChatService chatService;
//    private final ChatMessageService chatMessageService;
//    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송을 위해 추가
//
//    @MessageMapping("/chat.sendMessage")
//    public void sendMessage(@Payload ChatMessageDTO message) {
//        // redisPublisher.publish("chatroom", message); // RabbitMQ 사용으로 제거
//        chatMessageService.saveMessage(message);
//        // 메시지를 해당 채팅방 토픽으로 전송
//        // 클라이언트가 /topic/chat/{roomId}를 구독한다고 가정
//        if (message.getRoomId() != null && !message.getRoomId().isEmpty()) {
//            messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
//            log.info("Message sent to /topic/chat/{} via RabbitMQ: {}", message.getRoomId(), message);
//        } else {
//            // roomId가 없는 경우, 일반적인 "chatroom" 또는 다른 공용 토픽으로 보낼 수 있음
//            // messagingTemplate.convertAndSend("/topic/chatroom", message);
//            log.warn("Message without roomId, not sent to specific room topic: {}", message);
//        }
//    }
//
//    @GetMapping("/chat/history/{roomId}")
//    public List<ChatMessage> getChatHistory(@PathVariable String roomId) {
//        return chatService.getChatHistory(roomId);
//    }
//
////    @MessageMapping("/message") // 클라이언트에서 /app/message로 보낸 요청 처리
////    @SendTo("/topic/messages") // 결과를 /topic/messages로 브로드 캐스팅
////    public MessageDTO sendMessage(@Payload MessageDTO dto) {
////        log.info("받은 메세지 {}", dto);
////        return dto;
////    }
//
//
//}
