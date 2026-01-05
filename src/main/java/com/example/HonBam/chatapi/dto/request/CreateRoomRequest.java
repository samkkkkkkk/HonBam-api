package com.example.HonBam.chatapi.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CreateRoomRequest {
    private String name; // 방 이름

    @JsonProperty("isOpen")
    private boolean open;

    private boolean allowJoinAll;

    private List<String> participantIds;
}
