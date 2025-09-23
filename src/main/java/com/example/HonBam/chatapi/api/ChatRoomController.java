package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomListResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<?> chatRoomList(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        List<ChatRoomListResponseDTO> chatRoomList = chatRoomService.roomList(userInfo);
        return ResponseEntity.ok().body(chatRoomList);
    }

    @PostMapping("/direct")
    public ResponseEntity<ChatRoomResponse> startDirectChat(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam String targetUserId
    ) {
        ChatRoomResponse response = chatRoomService.startDirectChat(userInfo.getUserId(), targetUserId);
        return ResponseEntity.ok(response);
    }
}
