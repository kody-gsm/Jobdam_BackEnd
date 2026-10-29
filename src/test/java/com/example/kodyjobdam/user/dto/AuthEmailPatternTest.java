package com.example.kodyjobdam.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEmailPatternTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"s25035@gsm.hs.kr", "teacher@gsm.hs.kr", "wee.teacher@gmail.com"})
    void 비밀번호_재설정_코드는_학생_메일이_아니어도_받는다(String email) {
        PasswordResetCodeRequest request = new PasswordResetCodeRequest();
        ReflectionTestUtils.setField(request, "email", email);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"s25035@gsm.hs.kr", "teacher@gsm.hs.kr", "wee.teacher@gmail.com"})
    void 비밀번호_재설정은_학생_메일이_아니어도_받는다(String email) {
        PasswordResetRequest request = new PasswordResetRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "verificationCode", "123456");
        ReflectionTestUtils.setField(request, "newPassword", "new-password-1234");

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher@gsm.hs.kr", "s25035@gmail.com"})
    void 회원가입_인증_코드는_학생_메일만_받는다(String email) {
        EmailVerificationRequest request = new EmailVerificationRequest();
        ReflectionTestUtils.setField(request, "email", email);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher@gsm.hs.kr", "s25035@gmail.com"})
    void 회원가입은_학생_메일만_받는다(String email) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", "new-password-1234");
        ReflectionTestUtils.setField(request, "verificationCode", "123456");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("email");
    }
}
