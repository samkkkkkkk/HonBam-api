package com.example.HonBam.chatapi.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpenChatRoomResponseDTO {
    private String roonId;
    private String name;
    private int participantCont;
    private LocalDateTime lastMessageTime;
}
