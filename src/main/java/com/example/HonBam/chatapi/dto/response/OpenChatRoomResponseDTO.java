package com.example.HonBam.chatapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpenChatRoomResponseDTO {
    private String roomUuid;
    private String name;
    private int participantCount;
    private LocalDateTime lastMessageTime;

    @JsonProperty("isOpen")
    private boolean open;

    private boolean allowJoinAll;
}
