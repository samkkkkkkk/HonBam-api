package com.example.HonBam.chatapi.api;

// import com.example.HonBam.chatapi.component.RedisPublisher; // RabbitMQ 사용으로 RedisPublisher 제거

import com.example.HonBam.chatapi.dto.request.ChatMessageRequestDTO;
import com.example.HonBam.chatapi.dto.response.ChatMessageResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatUserResponseDTO;
import com.example.HonBam.chatapi.service.ChatMessageService;
import com.example.HonBam.chatapi.service.ChatRoomService;
import com.example.HonBam.chatapi.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatRoomService chatRoomService;
    // private final RedisPublisher redisPublisher; // RabbitMQ 사용으로 제거
    private final ChatMessageService chatMessageService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송을 위해 추가

//     특정 채팅방(roomId)으로 메세지를 전달
//    @MessageMapping("/chat/{roomId}")
//    @SendTo("/topic/chatroom/{roomId}")
//    public ChatMessageResponseDTO sendMessage(@DestinationVariable String roomId, ChatMessageRequestDTO message) {
//        return message;
//    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageRequestDTO message) {

        log.info("Received message: {}", message);
        // 채팅방에 속한 사용자만 메세지를 보낼 수 있도록 검증
        if (!chatRoomService.isUserInChatRoom(message.getRoomId(), message.getSenderId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방에 속한 사용자만 메시지를 보낼 수 있습니다.");
        }

        // 메세지를 데이터 베이스에도 저장
        chatMessageService.saveMessage(message);

        log.info("메시지 ID {}", message.getRoomId());

        // RabbitMQ를 통해 해당 채팅방 토픽으로 메시지 전송
        messagingTemplate.convertAndSend("/topic/chat." + message.getRoomId(), message);
        log.info("Message sent to /topic/chat/{} via RabbitMQ: {}", message.getRoomId(), message);

    }

    // 사용자 목록 조회 API
    @GetMapping("/chat/users")
    public ResponseEntity<?> getUsersInServer(@RequestParam(value = "exclude") String currentId) {
        List<ChatUserResponseDTO> users = chatService.findUsers(currentId);
        return ResponseEntity.ok().body(users);

    }

    // 채팅 목록 불러오기
    @GetMapping("/chat/history/{roomId}")
    public ResponseEntity<?> getChatHistory(@PathVariable(name = "roomId") String roomId) {

        log.info("Request to fetch chat history for room: {}", roomId);
        List<ChatMessageResponseDTO> chatHistory = chatService.getChatHistory(roomId);
        if (chatHistory.isEmpty()) {
            return ResponseEntity.noContent().build(); // 채팅 기록이 없을 경우 204 No Content 반환
        }
        return ResponseEntity.ok().body(chatHistory);
    }
}
