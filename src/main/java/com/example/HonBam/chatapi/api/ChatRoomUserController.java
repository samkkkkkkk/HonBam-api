package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.response.ChatRoomUserResponse;
import com.example.HonBam.chatapi.service.ChatRoomUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomUserController {

    private final ChatRoomUserService chatRoomUserService;

    @PostMapping("/{roomUuid}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomUuid,
                                      @AuthenticationPrincipal TokenUserInfo userInfo) {
        chatRoomUserService.joinRoom(roomUuid, userInfo);
        return ResponseEntity.ok().body("입장 성공");
    }

    @PostMapping("/{roomUuid}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomUuid,
                                       @AuthenticationPrincipal TokenUserInfo userInfo) {
        chatRoomUserService.leaveRoom(roomUuid, userInfo);
        return ResponseEntity.ok().body("퇴장 성공");
    }

    @GetMapping("/{roomUuid}/participants")
    public ResponseEntity<List<ChatRoomUserResponse>> getParticipants(@PathVariable String roomUuid) {
        return ResponseEntity.ok(chatRoomUserService.getParticipants(roomUuid));
    }
}
