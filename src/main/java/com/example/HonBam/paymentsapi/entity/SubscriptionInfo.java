package com.example.HonBam.paymentsapi.entity;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "subscription_info")
public class SubscriptionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subInfoId;

    @NotNull
    private LocalDateTime dueDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
