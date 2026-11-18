package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.EmailVerificationCodeRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.EmailVerificationRequest;
import com.example.kodyjobdam.user.dto.SignupRequest;
import com.example.kodyjobdam.user.dto.WithdrawalRequest;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
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

    @Mock
    private AccountWithdrawalCleanupService withdrawalCleanupService;

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

    @Test
    void 탈퇴하면_가입_정보와_토큰을_초기화하고_DataGSM_정보는_유지한다() {
        User user = User.builder()
                .id(10L)
                .dataGsmStudentId(20L)
                .name("김학생")
                .email("student@gsm.hs.kr")
                .student_number("3101")
                .grade(3)
                .classNum(1)
                .number(1)
                .role(UserRole.STUDENT)
                .password("encoded-password")
                .emailVerified(true)
                .refreshToken("old-refresh-token-hash")
                .refreshTokenExpiresAt(java.time.LocalDateTime.now().plusDays(1))
                .profileImageUrl("/uploads/profile-images/photo.jpg")
                .discordUserId("discord-123")
                .tutorialCompleted(true)
                .build();
        when(securityUtil.getCurrentUserId()).thenReturn(10L);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.withdraw(new WithdrawalRequest("password"));
            verify(profileImageStorageService, never()).delete("/uploads/profile-images/photo.jpg");
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(user.getPassword()).isNull();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getRefreshToken()).isNull();
        assertThat(user.getRefreshTokenExpiresAt()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
        assertThat(user.getDiscordUserId()).isNull();
        assertThat(user.isTutorialCompleted()).isFalse();
        assertThat(user.getAuthTokenVersion()).isEqualTo(1);
        assertThat(user.getDataGsmStudentId()).isEqualTo(20L);
        assertThat(user.getStudent_number()).isEqualTo("3101");
        assertThat(user.getGrade()).isEqualTo(3);
        assertThat(user.getRole()).isEqualTo(UserRole.STUDENT);
        verify(withdrawalCleanupService).deleteRecords(user);
        verify(emailVerificationCodeRepository).deleteByEmail("student@gsm.hs.kr");
        verify(userRepository).saveAndFlush(user);
        verify(profileImageStorageService).delete("/uploads/profile-images/photo.jpg");
    }

    @Test
    void 비밀번호가_틀리면_탈퇴하지_않는다() {
        User user = User.builder().id(10L).name("김학생").email("student@gsm.hs.kr")
                .password("encoded-password").emailVerified(true).role(UserRole.STUDENT).build();
        when(securityUtil.getCurrentUserId()).thenReturn(10L);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.withdraw(new WithdrawalRequest("wrong")))
                .hasMessage("비밀번호가 올바르지 않습니다.");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.isEmailVerified()).isTrue();
        verifyNoInteractions(emailVerificationCodeRepository);
        verifyNoInteractions(withdrawalCleanupService);
    }
}
