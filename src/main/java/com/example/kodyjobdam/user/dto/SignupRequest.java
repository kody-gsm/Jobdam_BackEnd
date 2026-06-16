package com.example.kodyjobdam.user.dto;

import com.example.kodyjobdam.user.UserRole;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @Email
    @NotBlank
    @Pattern(
            regexp = "^s.*@gsm\\.hs\\.kr$",
            message = "이메일은 s로 시작하고 @gsm.hs.kr로 끝나야 합니다"
    )
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String student_number;

    @NotNull
    private UserRole role;
}
