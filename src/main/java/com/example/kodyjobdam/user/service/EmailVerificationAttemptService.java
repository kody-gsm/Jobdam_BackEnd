package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.EmailVerificationCodeRepository;
import com.example.kodyjobdam.user.entity.EmailVerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long verificationCodeId, int maxFailedAttempts) {
        EmailVerificationCode verificationCode = emailVerificationCodeRepository
                .findByIdAndUsedAtIsNull(verificationCodeId)
                .orElse(null);
        if (verificationCode == null) {
            return;
        }

        verificationCode.setFailedAttempts(verificationCode.getFailedAttempts() + 1);
        if (verificationCode.getFailedAttempts() >= maxFailedAttempts) {
            verificationCode.setUsedAt(LocalDateTime.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUsed(Long verificationCodeId) {
        emailVerificationCodeRepository.findByIdAndUsedAtIsNull(verificationCodeId)
                .ifPresent(code -> code.setUsedAt(LocalDateTime.now()));
    }
}
