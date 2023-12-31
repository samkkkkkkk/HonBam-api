package com.example.HonBam.userapi.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@ToString
@EqualsAndHashCode(of = "userId")
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "hb_user")
public class User {

    @Id
//    @Column(name = "user_id")
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String userId; // 계정명이 아니라 식별 코드

    @Column(nullable = false, unique = true)
    private String email;


    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String userName;

    private String address;

    @CreationTimestamp
    private LocalDateTime joinDate;

    @Enumerated(EnumType.STRING)
//    @ColumnDefault("'COMMON'")
    @Builder.Default
    private Role role = Role.COMMON; // 유저 권한

    @Enumerated(EnumType.STRING)
    //    @ColumnDefault("'COMMON'")
    @Builder.Default
    private UserPay userPay = UserPay.NORMAL;

    private String profileImg; // 프로필 이미지 경로

    private String accessToken; // 카카오 로그인시 발급받는 accessToken을 저장 -> 로그아웃 때 필요





    // 등급 수정 메서드
    public void changeRole(Role role) {
        this.role = role;
    }

    public void changeUserPay(UserPay userPay) {
        this.userPay = userPay;
    }

    public void setAccessToken(String accessToken){
        this.accessToken = accessToken;
    }
}













