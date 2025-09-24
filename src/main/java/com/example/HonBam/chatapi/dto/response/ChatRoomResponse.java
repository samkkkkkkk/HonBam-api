package com.example.HonBam.chatapi.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    private String roomId;    // UUID만 클라이언트에 노출
    private String name;
    private String ownerId;
    private boolean isDirect;
    private boolean isOpen;
    private boolean allowJoinAll;
    private LocalDateTime createdAt;
}
