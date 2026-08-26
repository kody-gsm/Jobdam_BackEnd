package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.entity.EmailVerificationPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.email-verification.code-expiration-minutes:10}")
    private long codeExpirationMinutes;

    public void sendVerificationCode(String email, String code, EmailVerificationPurpose purpose) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null || from.isBlank()) {
            log.info("Email verification code for {} ({}): {}", email, purpose, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("JOBDAM");
        message.setText("JOBDAM인증코드 : " + code
                + ". It expires in " + codeExpirationMinutes + " minutes.");
        mailSender.send(message);
    }
}
