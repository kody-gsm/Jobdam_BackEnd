package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {

    @Email
    @NotBlank
    @Pattern(
            regexp = "^s.*@gsm\\.hs\\.kr$",
            message = "이메일은 학교계정을 사용해야합니다."
    )
    private String email;
}
