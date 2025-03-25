package com.example.HonBam.chatapi.api;

import com.example.HonBam.chatapi.dto.ChatRoomRequest;
import com.example.HonBam.chatapi.dto.InviteRequest;
import com.example.HonBam.chatapi.dto.InviteRequset;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 생성 api
    @PostMapping
    public ResponseEntity<ChatRoom> createChatRoom(@RequestBody ChatRoomRequest request) {
        ChatRoom chatRoom = chatRoomService.createChatRoom(request.getName(), request.getUserIds());
        return ResponseEntity.ok(chatRoom);
    }

    // 특정 채팅방에 사용자를 초대하는 API
    @PostMapping("/{chatRoomId}/invite")
    public ResponseEntity<?> inviteUser(@PathVariable Long chatRoomId, @RequestBody InviteRequest request) {
        chatRoomService.inviteUser(chatRoomId, request.getUserId());


    }

}
