package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.entity.EmailVerificationPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.email.from-name:JOBDAM}")
    private String fromName;

    @Value("${app.email.reply-to:}")
    private String replyTo;

    @Value("${app.email-verification.code-expiration-minutes:10}")
    private long codeExpirationMinutes;

    public void sendVerificationCode(String email, String code, EmailVerificationPurpose purpose) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null || from.isBlank()) {
            log.info("Email verification code for {} ({}): {}", email, purpose, code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(email);
            if (!replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(createSubject(purpose));
            helper.setText(createPlainText(code, purpose), createHtmlText(code, purpose));

            message.setHeader("Auto-Submitted", "auto-generated");
            message.setHeader("X-Auto-Response-Suppress", "All");

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Failed to send email verification code to {} ({})", email, purpose, e);
            throw new IllegalStateException("인증 메일 발송에 실패했습니다.", e);
        }
    }

    private String createSubject(EmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "[JOBDAM] 회원가입 인증코드";
            case PASSWORD_RESET -> "[JOBDAM] 비밀번호 재설정 인증코드";
        };
    }

    private String createPlainText(String code, EmailVerificationPurpose purpose) {
        return """
                JOBDAM %s

                인증코드: %s
                유효시간: %d분

                본인이 요청하지 않았다면 이 메일은 무시해 주세요.
                """.formatted(createPurposeLabel(purpose), code, codeExpirationMinutes);
    }

    private String createHtmlText(String code, EmailVerificationPurpose purpose) {
        String purposeLabel = createPurposeLabel(purpose);

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>JOBDAM 인증코드</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#172033;">
                    <div style="display:none;max-height:0;overflow:hidden;color:#f4f7fb;">
                        JOBDAM 인증코드는 %s이며 %d분 동안 유효합니다.
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f7fb;padding:32px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border:1px solid #e5ebf3;border-radius:8px;overflow:hidden;">
                                    <tr>
                                        <td style="padding:28px 32px 18px;background:#123b7a;color:#ffffff;">
                                            <div style="font-size:13px;font-weight:700;letter-spacing:0;">JOBDAM</div>
                                            <h1 style="margin:16px 0 0;font-size:24px;line-height:1.35;font-weight:700;">%s</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:30px 32px 10px;">
                                            <p style="margin:0 0 18px;font-size:15px;line-height:1.7;color:#344054;">
                                                아래 인증코드를 JOBDAM 화면에 입력해 주세요.
                                            </p>
                                            <div style="margin:0 0 22px;padding:22px 16px;background:#f8fafc;border:1px solid #dbe4ee;border-radius:8px;text-align:center;">
                                                <div style="font-size:13px;color:#667085;margin-bottom:8px;">인증코드</div>
                                                <div style="font-size:34px;line-height:1;font-weight:800;letter-spacing:6px;color:#123b7a;">%s</div>
                                            </div>
                                            <p style="margin:0 0 18px;font-size:14px;line-height:1.7;color:#475467;">
                                                이 코드는 <strong style="color:#172033;">%d분</strong> 동안만 유효합니다.
                                            </p>
                                            <p style="margin:0;font-size:13px;line-height:1.7;color:#667085;">
                                                본인이 요청하지 않았다면 이 메일은 안전하게 무시해 주세요.
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:18px 32px 28px;">
                                            <div style="height:1px;background:#e5ebf3;margin-bottom:18px;"></div>
                                            <p style="margin:0;font-size:12px;line-height:1.6;color:#98a2b3;">
                                                이 메일은 JOBDAM 인증 요청에 따라 자동 발송되었습니다.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(code, codeExpirationMinutes, purposeLabel, code, codeExpirationMinutes);
    }

    private String createPurposeLabel(EmailVerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "회원가입 인증";
            case PASSWORD_RESET -> "비밀번호 재설정 인증";
        };
    }
}
