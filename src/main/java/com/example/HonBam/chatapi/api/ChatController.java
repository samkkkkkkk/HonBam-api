package com.example.HonBam.chatapi.api;

import com.example.HonBam.chatapi.component.RedisPublisher;
import com.example.HonBam.chatapi.dto.ChatMessageDTO;
import com.example.HonBam.chatapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final RedisPublisher redisPublisher;

//     특정 채팅방(roomId)으로 메세지를 전달
//    @MessageMapping("/chat/{roomId}")
//    @SendTo("/topic/chatroom/{roomId}")
//    public ChatMessageDTO sendMessage(@DestinationVariable String roomId, ChatMessageDTO message) {
//        return message;
//    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageDTO message) {
        // 채팅방에 속한 사용자만 메세지르르 보낼 수 있도록 검증
        if (!chatRoomService.isUserInChatRoom(message.getRoomId(), message.getSender())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방에 속한 사용자만 메시지를 보낼 수 있습니다.");
        }

        // 메세지를 데이터 베이스에도 저장
        chatMessageService.save(message);

        // 초대된 사용자들에게만 메세지 전송
        redisPublisher.publish("chat-room" + message.getRoomId(), message);


    }
}
