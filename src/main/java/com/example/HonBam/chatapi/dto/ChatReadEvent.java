package com.example.HonBam.chatapi.dto;

import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadEvent {
    private String roomUuid;
    private Long messageId;
    private Long unReadUserCount;
    private String readerId;
}
