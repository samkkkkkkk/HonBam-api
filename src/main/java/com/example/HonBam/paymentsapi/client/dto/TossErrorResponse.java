package com.example.HonBam.paymentsapi.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Toss Payments API 오류 응답 본문 {code, message}
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TossErrorResponse {

    private String code;
    private String message;

}
