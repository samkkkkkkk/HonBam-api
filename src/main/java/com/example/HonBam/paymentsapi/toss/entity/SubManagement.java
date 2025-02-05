package com.example.HonBam.paymentsapi.toss.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "sub_management")
public class SubManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_management_id")
    private Long subManagementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_id")
    private PaidInfo paidInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_id")
    private Subscription subscription;


}
