package com.example.HonBam.paymentsapi.dto.response;

import com.example.HonBam.paymentsapi.entity.PaidInfo;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentResponseDTO {

    private String orderName;
    private LocalDateTime paidAt;
    private String OrderId;
    private String method;
    private int amount;

    @Builder.Default
    private int discount = 0;

    public TossPaymentResponseDTO(PaidInfo save) {
        this.orderName = save.getOrderName();
        this.paidAt = save.getRequestedAt();
        this.OrderId = save.getOrderId();
        this.method = save.getMethod();
        this.amount = save.getAmount();
    }
}
