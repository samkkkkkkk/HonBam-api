package com.example.HonBam.userapi.dto.request;

import com.example.HonBam.userapi.entity.User;
import lombok.*;

import javax.persistence.Column;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter @Getter
@ToString @EqualsAndHashCode(of = "email")
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserRequestSignUpDTO {

    @NotBlank
    @Email
    private String email;

    private String userId;

    private String phoneNumber;

    @NotBlank
    @Size(min = 8, max = 20)
    private String password;

    @NotBlank
    @Size(min = 2, max = 6)
    private String userName;


    private String address;

    // dto를 Entity로 변경하는 메서드
    public User toEntity(String uploadedFilePath) {
        return User.builder()
                .email(this.email)
                .phoneNumber(this.phoneNumber)
                .password(this.password)
                .userName(this.userName)
                .address(this.address)
                .userId(this.userId)
                .profileImg(uploadedFilePath)
                .build();
    }


}












