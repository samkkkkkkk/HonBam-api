package com.example.HonBam.paymentsapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@ToString @EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_info")
@Builder
public class PaymentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payId;

    @NotNull
    private int amount;

    @NotNull
    private String orderId;

    @NotNull
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime create_at = LocalDateTime.now();

}
