package com.example.HonBam;

import com.example.HonBam.paymentsapi.toss.util.MessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class WebSocketController {

    @MessageMapping("/message") // 클라이언트에서 /app/message로 보낸 요청 처리
    @SendTo("/topic/messages") // 결과를 /topic/messages로 브로드 캐스팅
    public MessageDTO sendMessage(@Payload MessageDTO dto) {
        log.info("받은 메세지 {}", dto);
        return dto;
    }


}
