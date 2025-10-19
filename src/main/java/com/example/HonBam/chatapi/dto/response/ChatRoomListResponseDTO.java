package com.example.HonBam.chatapi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String roomUuid;
    private String customName;
    private String ownerId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unReadCount;
    private Long lastMessageId;
    
    // 사용자별 읽음 커서
    private Long lastReadMessageId;

    @JsonProperty("isDirect")
    private boolean direct;

    @JsonProperty("isOpen")
    private boolean open;

    private boolean allowJoinAll;

}
