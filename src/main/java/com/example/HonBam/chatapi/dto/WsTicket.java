package com.example.HonBam.chatapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WsTicket {
    private String ticket;     // 임시 티켓(UUID)
    private String userId;     // 발급 대상 사용자
    private Instant expiredAt; // 만료 시각
}