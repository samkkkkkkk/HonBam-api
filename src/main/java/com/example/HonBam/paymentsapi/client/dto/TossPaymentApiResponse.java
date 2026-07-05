package com.example.HonBam.paymentsapi.client.dto;

import com.example.HonBam.paymentsapi.entity.PaidInfo;
import com.example.HonBam.paymentsapi.entity.TossPaymentStatus;
import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

// Toss Payments API 응답(승인/취소/조회) 매핑용
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentApiResponse {

    private String paymentKey;
    private String orderId;

    @JsonProperty("totalAmount")
    private Integer amount;

    @JsonProperty("status")
    private TossPaymentStatus paymentStatus;

    private String orderName;

    private String method;

    private LocalDateTime requestedAt;

    private VirtualAccount virtualAccount;

    @Getter
    @ToString
    public static class VirtualAccount {
        private String customerName;
        @JsonProperty("bankCode")
        private String bank;
        private String accountNumber;
    }

    // 토스의 requestedAt(ISO offset 문자열)을 LocalDateTime으로 변환
    @JsonProperty("requestedAt")
    public void setRequestedAt(String requestedAt) {
        this.requestedAt = OffsetDateTime.parse(requestedAt).toLocalDateTime();
    }

    public PaidInfo toEntity(User user) {
        boolean isVirtualAccount = this.virtualAccount != null;
        return PaidInfo.builder()
                .paymentKey(this.paymentKey)
                .orderId(this.orderId)
                .amount(this.amount)
                .paymentStatus(this.paymentStatus)
                .orderName(this.orderName)
                .method(this.method)
                .requestedAt(this.requestedAt)
                .customerName(isVirtualAccount ? this.virtualAccount.customerName : user.getUserName())
                .bank(isVirtualAccount ? this.virtualAccount.bank : null)
                .accountNumber(isVirtualAccount ? this.virtualAccount.accountNumber : null)
                .user(user)
                .build();
    }

}
