package com.example.HonBam.paymentsapi.dto.request;

import com.example.HonBam.paymentsapi.entity.PaymentInfo;
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


    // 결제 요청이 들어왔을 때 저장할 결제 정보 (payId가 null이면 신규 저장, 있으면 갱신)
    public PaymentInfo toEntity(Long payId) {
        return PaymentInfo.builder()
                .payId(payId)
                .amount(this.amount)
                .orderId(this.orderId)
                .method(this.method)
                .build();
    }

}
