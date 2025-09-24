package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.CreateRoomRequest;
import com.example.HonBam.chatapi.dto.request.InviteUserRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomListResponseDTO;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponse;
import com.example.HonBam.chatapi.dto.response.OpenChatRoomResponseDTO;
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

    // 채팅방 생성
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        return ResponseEntity.ok(chatRoomService.createRoom(userInfo.getUserId(), request));
    }

    // 내가 속한 채팅방 목록 확인
    @GetMapping
    public ResponseEntity<?> chatRoomList(
            @AuthenticationPrincipal TokenUserInfo userInfo
    ) {
        List<ChatRoomListResponseDTO> chatRoomList = chatRoomService.roomList(userInfo);
        return ResponseEntity.ok().body(chatRoomList);
    }

    // 1대1 채팅 시작
    @PostMapping("/direct")
    public ResponseEntity<ChatRoomResponse> startDirectChat(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam String targetUserId
    ) {
        ChatRoomResponse response = chatRoomService.startDirectChat(userInfo.getUserId(), targetUserId);
        return ResponseEntity.ok(response);
    }

    // 초대 방식
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<?> inviteUser(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String roomUuid,
            @RequestBody InviteUserRequest request
    ) {
        chatRoomService.inviteUser(roomUuid, userInfo.getUserId(), request.getTargetUserIds());
        return ResponseEntity.ok("초대 성공");
    }

    // 자유입장 방식
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinOpenRoom(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable String roomUuid
    ) {
        chatRoomService.joinOpenRoom(roomUuid, userInfo.getUserId());
        return ResponseEntity.ok("참여완료");
    }

    // 오픈 채팅방 리스트
    @GetMapping("/open")
    public ResponseEntity<List<OpenChatRoomResponseDTO>> openChatRoomList(@RequestParam(required = false) String keyword) {
        List<OpenChatRoomResponseDTO> rooms = chatRoomService.findOpenRooms(keyword);
        return ResponseEntity.ok(rooms);
    }
}
