package com.example.HonBam.chatapi.dto.request;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDTO {

    private String roomId;
    private String senderId;
    private String message;

}
