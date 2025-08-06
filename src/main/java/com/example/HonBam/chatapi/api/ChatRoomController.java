package com.example.HonBam.chatapi.api;

import com.example.HonBam.auth.TokenUserInfo;
import com.example.HonBam.chatapi.dto.request.ChatRoomRequest;
import com.example.HonBam.chatapi.dto.response.ChatRoomResponseDTO;
import com.example.HonBam.chatapi.dto.request.InviteRequest;
import com.example.HonBam.chatapi.entity.ChatRoom;
import com.example.HonBam.chatapi.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 생성 api
    @PostMapping
    public ResponseEntity<ChatRoomResponseDTO> createChatRoom(@RequestBody ChatRoomRequest request) {
        ChatRoomResponseDTO newChatRoomDto = chatRoomService.createChatRoom(request.getName(), request.getUserIds());
        // 생성된 리소스의 URI를 생성
        // 생성된 리소스의 URI를 생성
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest() // 현재 요청 (/chatrooms)
                .path("/{id}")        // PathVariable {id} 추가
                .buildAndExpand(newChatRoomDto.getId()) // DTO의 ID로 {id} 값 채우기
                .toUri();

        return ResponseEntity.created(location).body(newChatRoomDto);
    }

    // 특정 채팅방에 사용자를 초대하는 API
    @PostMapping("/{chatRoomId}/invite")
    public ResponseEntity<?> inviteUser(@PathVariable Long chatRoomId, @RequestBody InviteRequest request) {
        // TODO: ChatRoomService.inviteUser 내부에서 발생할 수 있는 예외
        // (e.g., ChatRoomNotFoundException, UserNotFoundException, UserAlreadyInRoomException 등)에 대한
        // 처리를 고려하여 @ExceptionHandler 또는 @ControllerAdvice를 통한 응답 커스터마이징을 고려할 수 있습니다.
        chatRoomService.inviteUser(chatRoomId, request.getUserId());
        return ResponseEntity.ok().build();

    }

    @GetMapping("/myrooms")
    public ResponseEntity<?> getMyChatRooms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Request to fetch chat rooms for user: {}", userInfo.getEmail());
        List<ChatRoomResponseDTO> myRooms = chatRoomService.findMyChatRooms(userInfo.getEmail());
        return ResponseEntity.ok().body(myRooms);
    }




}
