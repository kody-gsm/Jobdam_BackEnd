package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.EmailVerificationCodeRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.EmailVerificationRequest;
import com.example.kodyjobdam.user.dto.SignupRequest;
import com.example.kodyjobdam.user.entity.EmailVerificationCode;
import com.example.kodyjobdam.user.entity.EmailVerificationPurpose;
import com.example.kodyjobdam.user.entity.User;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import com.example.kodyjobdam.user.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEACHER_EMAIL = "teacher@gsm.hs.kr";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailVerificationAttemptService emailVerificationAttemptService;

    @Mock
    private DataGsmStudentSyncService dataGsmStudentSyncService;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ProfileImageStorageService profileImageStorageService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "emailCodeExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "emailCodeMaxFailedAttempts", 5);
    }

    @Test
    void 미리_넣어_둔_선생님_계정은_s로_시작하지_않아도_가입하고_역할을_유지한다() {
        User teacher = User.builder()
                .name("김선생")
                .student_number("")
                .email(TEACHER_EMAIL)
                .role(UserRole.TEACHER)
                .build();
        when(userRepository.findByEmail(TEACHER_EMAIL)).thenReturn(Optional.of(teacher));

        EmailVerificationRequest codeRequest = new EmailVerificationRequest();
        codeRequest.setEmail(TEACHER_EMAIL);
        authService.sendSignupVerificationCode(codeRequest);

        ArgumentCaptor<EmailVerificationCode> savedCode = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(emailVerificationCodeRepository).save(savedCode.capture());
        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(emailVerificationService)
                .sendVerificationCode(eq(TEACHER_EMAIL), sentCode.capture(), eq(EmailVerificationPurpose.SIGNUP));

        when(emailVerificationCodeRepository.findTopByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                TEACHER_EMAIL, EmailVerificationPurpose.SIGNUP)).thenReturn(Optional.of(savedCode.getValue()));
        when(passwordEncoder.encode("teacher-password-1!")).thenReturn("encoded-password");
        when(userRepository.save(teacher)).then(returnsFirstArg());

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail(TEACHER_EMAIL);
        signupRequest.setPassword("teacher-password-1!");
        signupRequest.setVerificationCode(sentCode.getValue());
        authService.signup(signupRequest);

        assertThat(teacher.getRole()).isEqualTo(UserRole.TEACHER);
        assertThat(teacher.getPassword()).isEqualTo("encoded-password");
        assertThat(teacher.isEmailVerified()).isTrue();
        verifyNoInteractions(dataGsmStudentSyncService);
    }

    @Test
    void DB와_DataGSM_어디에도_없는_학교_메일에는_가입_코드를_보내지_않는다() {
        when(userRepository.findByEmail(TEACHER_EMAIL)).thenReturn(Optional.empty());
        when(dataGsmStudentSyncService.syncStudentByEmail(TEACHER_EMAIL)).thenReturn(null);

        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setEmail(TEACHER_EMAIL);
        authService.sendSignupVerificationCode(request);

        verifyNoInteractions(emailVerificationCodeRepository, emailVerificationService);
    }
}
