package com.example.HonBam.userapi.dto.response;

import com.example.HonBam.userapi.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;

// 로그인 성공 후 클라이언트에게 전송할 데이터 객체
@Getter
@ToString @EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String id;
    private String email;
    private String userName;


    @JsonFormat(pattern = "yyyy년 MM월 dd일")
    private LocalDate joinDate;

    private String role;
    private String userPay;
    private String address;
    private String phoneNumber;
    private String nickname;



    public LoginResponseDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.userName = user.getUserName();
        // user.getJoinDate() 타입이 LocalDateTime이면 toLocalDate()로 변환
        this.joinDate = LocalDate.from(user.getJoinDate());
        this.role = String.valueOf(user.getRole());
        this.userPay = String.valueOf(user.getUserPay());
        this.address = user.getAddress();
        this.phoneNumber = user.getPhoneNumber();
        this.nickname = user.getNickname();
    }
}







