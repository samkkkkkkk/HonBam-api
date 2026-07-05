package com.example.HonBam.paymentsapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@EqualsAndHashCode(of = "paidId")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paid_info")
public class PaidInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paidId;

    @NotNull
    private int amount;

    @NotNull
    private String orderId;

    @NotNull
    private String orderName;


    private String customerName;

    private String bank;

    private String accountNumber;

    @NotNull
    private String method;

    @NotNull
    private String paymentKey;

    @NotNull
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TossPaymentStatus paymentStatus;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    public void applyCancellation(TossPaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

}
