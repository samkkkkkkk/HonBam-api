package com.example.HonBam.paymentsapi.toss.dto.requestDTO;

import com.example.HonBam.paymentsapi.toss.entity.PaidInfo;
import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TosspaymentRequestDTO {

    private String paymentKey;
    private String orderId;

    @JsonProperty("totalAmount")
    private Integer amount;

    @JsonProperty("status")
    private String paymentStatus;

    private String orderName;

    private String method;

    @JsonFormat(pattern = "yyyy년 MM월 dd일 HH:mm:ss")
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

    // 토스에서 넘어오는 requestedAt을 localDateTime으로 변환
    private void setRequestedAt(String requestedAt) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(requestedAt);
        this.requestedAt = offsetDateTime.toLocalDateTime();
    }

    // DTO를 Entity로 변경하는 과정
    public PaidInfo toEntityVirtualAccount(User user) {
        return PaidInfo.builder()
                .paymentKey(this.paymentKey)
                .orderId(this.orderId)
                .amount(this.amount)
                .paymentStatus(this.paymentStatus)
                .orderName(this.orderName)
                .method(this.method)
                .requestedAt(this.requestedAt)
                .customerName(this.virtualAccount.customerName)
                .bank(this.virtualAccount.bank)
                .accountNumber(this.virtualAccount.accountNumber)
                .user(user)
                .build();
    }


    public PaidInfo toEntity(User user) {
        return PaidInfo.builder()
                .paymentKey(this.paymentKey)
                .orderId(this.orderId)
                .amount(this.amount)
                .paymentStatus(this.paymentStatus)
                .orderName(this.orderName)
                .customerName(user.getUserName())
                .method(this.method)
                .requestedAt(this.requestedAt)
                .user(user)
                .build();
    }


    public PaidInfo toEntity(User user, Long payId) {
        return PaidInfo.builder()
                .paymentKey(this.paymentKey)
                .orderId(this.orderId)
                .amount(this.amount)
                .paymentStatus(this.paymentStatus)
                .orderName(this.orderName)
                .customerName(user.getUserName())
                .method(this.method)
                .requestedAt(this.requestedAt)
                .user(user)
                .build();
    }


}
