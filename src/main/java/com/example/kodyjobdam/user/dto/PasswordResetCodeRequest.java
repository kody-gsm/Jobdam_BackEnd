package com.example.kodyjobdam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 비밀번호 재설정 인증 코드 요청. 회원가입과 달리 학생 계정(s로 시작하는 학교 메일)으로 제한하지 않는다.
 * 가입된 계정에만 코드를 보내므로 선생님 계정도 비밀번호를 바꿀 수 있다.
 */
@Getter
@Setter
public class PasswordResetCodeRequest {

    @Email
    @NotBlank
    private String email;
}
