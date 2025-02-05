package com.example.HonBam.paymentsapi.toss.dto.requestDTO;

import lombok.*;

@Getter @Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmReqDTO {

    private String orderId; // 주문 번호
    private int amount; // 금액
    private String paymentKey; // 토스페이먼츠에서 전달하는 paymetKey 취소, 환불에 필요
    private String method; // 결제수단
    
    
}
