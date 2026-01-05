package com.example.HonBam.chatapi.entity;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatReadId implements Serializable {
    private Long messageId;
    private String userId;
}
