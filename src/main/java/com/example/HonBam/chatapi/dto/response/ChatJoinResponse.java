package com.example.HonBam.chatapi.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatJoinResponse {
    private String roomUuid;
    private Long lastReadMessageId;
}
