package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    // 선생님도 학교 메일로 가입할 수 있도록 s로 시작하는지는 보지 않는다. DB에 있거나 DataGSM에서 찾은 계정만 가입된다.
    @Email
    @NotBlank
    @Pattern(
            regexp = "^[^@]+@gsm\\.hs\\.kr$",
            message = "이메일은 학교계정을 사용해야합니다."
    )
    private String email;

    @NotBlank
    @Size(min = 10, max = 255)
    private String password;

    @NotBlank
    @Size(min = 6, max = 6)
    private String verificationCode;
}
