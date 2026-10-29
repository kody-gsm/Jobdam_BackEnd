package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {

    // 선생님 계정도 재설정할 수 있도록 학생 메일 형식으로 제한하지 않는다. 가입된 계정만 재설정된다.
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String verificationCode;

    @NotBlank
    @Size(min = 10, max = 255)
    private String newPassword;
}
