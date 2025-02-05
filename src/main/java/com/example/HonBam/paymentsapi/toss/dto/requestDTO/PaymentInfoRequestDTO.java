package com.example.HonBam.paymentsapi.toss.dto.requestDTO;

import com.example.HonBam.paymentsapi.toss.entity.PaymentInfo;
import com.example.HonBam.paymentsapi.toss.entity.PaymentStatus;
import com.example.HonBam.userapi.entity.User;
import lombok.*;

@Getter @Setter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfoRequestDTO {

    private int amount;
    private String orderId;
    private String method;
    private String subId;

    
    // 결제 요청이 들어왔을 때 저장할 결제 정보
    public PaymentInfo toEntity() {
        return PaymentInfo.builder()
                .amount(this.amount)
                .orderId(this.orderId)
                .method(this.method)
                .build();
    }


    public PaymentInfo toEntity(Long payId) {
        return PaymentInfo.builder()
                .payId(payId)
                .amount(this.amount)
                .orderId(this.orderId)
                .method(this.method)
                .build();
    }

}
