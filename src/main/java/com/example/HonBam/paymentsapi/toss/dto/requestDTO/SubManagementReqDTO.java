package com.example.HonBam.paymentsapi.toss.dto.requestDTO;

import com.example.HonBam.paymentsapi.toss.entity.PaidInfo;
import com.example.HonBam.paymentsapi.toss.entity.SubManagement;
import com.example.HonBam.paymentsapi.toss.entity.Subscription;
import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubManagementReqDTO {


    private PaidInfo paidInfo;

    private Subscription subscription;

    private User user;

    public SubManagement toEntity(Subscription subscription, PaidInfo paidInfo) {
        return SubManagement.builder()
                .paidInfo(paidInfo)
                .subscription(subscription)
                .build();
    }

}
