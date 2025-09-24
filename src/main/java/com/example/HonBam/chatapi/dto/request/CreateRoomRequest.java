package com.example.HonBam.chatapi.dto.request;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {
    private String name; // 방 이름
    private boolean isOpen;
    private boolean allowJoinAll;
    private List<String> participantIds;
}
