package com.example.HonBam.paymentsapi.dto.response;

import com.example.HonBam.paymentsapi.entity.Subscription;
import lombok.*;

@Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponseDTO {

    private Long subId;
    private int period;
    private int price;
    private String orderName;
    private String description;

    public static SubscriptionResponseDTO from(Subscription subscription) {
        return SubscriptionResponseDTO.builder()
                .subId(subscription.getSubId())
                .period(subscription.getPeriod())
                .price(subscription.getPrice())
                .orderName(subscription.getOrderName())
                .description(subscription.getDescription())
                .build();
    }

}
