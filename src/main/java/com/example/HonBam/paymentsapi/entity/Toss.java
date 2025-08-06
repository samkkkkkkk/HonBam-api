package com.example.HonBam.paymentsapi.entity;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "hb_toss")
public class Toss {

    @Id
    private Long id;
    private String title;

    private int amount;



    private String orderId;

}
