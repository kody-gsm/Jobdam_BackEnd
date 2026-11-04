package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {

    // 선생님도 학교 메일로 가입 인증 코드를 받을 수 있도록 s로 시작하는지는 보지 않는다.
    @Email
    @NotBlank
    @Pattern(
            regexp = "^[^@]+@gsm\\.hs\\.kr$",
            message = "이메일은 학교계정을 사용해야합니다."
    )
    private String email;
}
