package com.example.HonBam.chatapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    private String roomId;
    private String name;
    private String ownerId;

    @JsonProperty("isDirect")
    private boolean direct;

    @JsonProperty("isOpen")
    private boolean open;

    private boolean allowJoinAll;  // 이건 그대로 괜찮음

    private LocalDateTime createdAt;

    public boolean isDirect() { return direct; }
    public boolean isOpen() { return open; }
}
