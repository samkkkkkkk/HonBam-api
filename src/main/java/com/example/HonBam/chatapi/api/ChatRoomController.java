package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        return ResponseEntity.ok(chatRoomService.createRoom(request, userInfo));
    }
}
