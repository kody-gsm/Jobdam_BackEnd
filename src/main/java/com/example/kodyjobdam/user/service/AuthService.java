package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.EmailVerificationCodeRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.AuthResponse;
import com.example.kodyjobdam.user.dto.EmailVerificationRequest;
import com.example.kodyjobdam.user.dto.LoginRequest;
import com.example.kodyjobdam.user.dto.PasswordResetRequest;
import com.example.kodyjobdam.user.dto.RefreshTokenRequest;
import com.example.kodyjobdam.user.dto.SignupRequest;
import com.example.kodyjobdam.user.dto.UserProfileResponse;
import com.example.kodyjobdam.user.dto.UserResponse;
import com.example.kodyjobdam.user.entity.EmailVerificationCode;
import com.example.kodyjobdam.user.entity.EmailVerificationPurpose;
import com.example.kodyjobdam.user.entity.User;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import com.example.kodyjobdam.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final DataGsmStudentSyncService dataGsmStudentSyncService;
    private final SecurityUtil securityUtil;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.email-verification.code-expiration-minutes:10}")
    private long emailCodeExpirationMinutes;

    @Value("${app.email-verification.max-failed-attempts:5}")
    private int emailCodeMaxFailedAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UserResponse signup(SignupRequest request) {
        validateAndUseEmailCode(
                request.getEmail(),
                request.getVerificationCode(),
                EmailVerificationPurpose.SIGNUP
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> dataGsmStudentSyncService.syncStudentByEmail(request.getEmail()));

        if (user == null) {
            throw new IllegalArgumentException("Student information was not found.");
        }

        if (StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Already signed up.");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.STUDENT);
        user.setEmailVerified(true);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Invalid signup request.");
        }

        return UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .student_number(savedUser.getStudent_number())
                .name(savedUser.getName())
                .emailVerified(savedUser.isEmailVerified())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Email verification is required.");
        }

        String refreshToken = issueRefreshToken(user);
        return createAuthResponse(user, refreshToken);
    }

    @Transactional
    public AuthResponse reissue(RefreshTokenRequest request) {
        User user = findUserByRefreshToken(request.getRefreshToken());

        if (user.getRefreshTokenExpiresAt() == null
                || user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            clearRefreshToken(user);
            throw new IllegalArgumentException("Refresh token has expired.");
        }

        String refreshToken = issueRefreshToken(user);
        return createAuthResponse(user, refreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        User user = findUserByRefreshToken(request.getRefreshToken());
        clearRefreshToken(user);
    }

    @Transactional
    public void sendSignupVerificationCode(EmailVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> dataGsmStudentSyncService.syncStudentByEmail(request.getEmail()));

        if (user == null || StringUtils.hasText(user.getPassword())) {
            return;
        }

        issueEmailCode(request.getEmail(), EmailVerificationPurpose.SIGNUP);
    }

    @Transactional
    public void sendPasswordResetVerificationCode(EmailVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !StringUtils.hasText(user.getPassword())) {
            return;
        }

        issueEmailCode(request.getEmail(), EmailVerificationPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        validateAndUseEmailCode(
                request.getEmail(),
                request.getVerificationCode(),
                EmailVerificationPurpose.PASSWORD_RESET
        );

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        clearRefreshToken(user);
    }

    public UserProfileResponse getProfile() {
        Long userId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        return new UserProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getStudent_number()
        );
    }

    private AuthResponse createAuthResponse(User user, String refreshToken) {
        String accessToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    private User findUserByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(hashToken(refreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
    }

    private String issueRefreshToken(User user) {
        String refreshToken = createRefreshToken();
        user.setRefreshToken(hashToken(refreshToken));
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS));
        return refreshToken;
    }

    private void clearRefreshToken(User user) {
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
    }

    private void issueEmailCode(String email, EmailVerificationPurpose purpose) {
        String code = createEmailCode();
        LocalDateTime now = LocalDateTime.now();

        emailVerificationCodeRepository.markUnusedCodesAsUsed(email, purpose, now);

        EmailVerificationCode verificationCode = EmailVerificationCode.builder()
                .email(email)
                .purpose(purpose)
                .codeHash(hashToken(code))
                .expiresAt(now.plusMinutes(emailCodeExpirationMinutes))
                .createdAt(now)
                .build();

        emailVerificationCodeRepository.save(verificationCode);
        emailVerificationService.sendVerificationCode(email, code, purpose);
    }

    private void validateAndUseEmailCode(String email, String code, EmailVerificationPurpose purpose) {
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findTopByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationCode.setUsedAt(LocalDateTime.now());
            throw new IllegalArgumentException("Verification code has expired.");
        }

        if (!verificationCode.getCodeHash().equals(hashToken(code))) {
            verificationCode.setFailedAttempts(verificationCode.getFailedAttempts() + 1);
            if (verificationCode.getFailedAttempts() >= emailCodeMaxFailedAttempts) {
                verificationCode.setUsedAt(LocalDateTime.now());
            }
            throw new IllegalArgumentException("Invalid verification code.");
        }

        verificationCode.setUsedAt(LocalDateTime.now());
    }

    private String createEmailCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
