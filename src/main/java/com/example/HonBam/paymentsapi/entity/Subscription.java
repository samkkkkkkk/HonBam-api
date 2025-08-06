package com.example.HonBam.paymentsapi.entity;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@ToString @EqualsAndHashCode
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hb_subscription")
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subId;

    @NotNull
    private int period;

    @NotNull
    private int price;

    @NotNull
    private String orderName;

    private String description;



}
