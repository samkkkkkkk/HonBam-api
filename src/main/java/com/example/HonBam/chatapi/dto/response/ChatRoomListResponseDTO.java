package com.example.HonBam.chatapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListResponseDTO {
    private String roomId;
    private String name;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    int unReadCount;
}
