package com.example.kodyjobdam.user;

import com.example.kodyjobdam.user.entity.EmailVerificationCode;
import com.example.kodyjobdam.user.entity.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findTopByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EmailVerificationCode e
            set e.usedAt = :usedAt
            where e.email = :email
              and e.purpose = :purpose
              and e.usedAt is null
            """)
    int markUnusedCodesAsUsed(
            @Param("email") String email,
            @Param("purpose") EmailVerificationPurpose purpose,
            @Param("usedAt") LocalDateTime usedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from EmailVerificationCode e
            where e.expiresAt < :threshold
               or (e.usedAt is not null and e.usedAt < :threshold)
            """)
    int deleteExpiredOrUsedBefore(@Param("threshold") LocalDateTime threshold);
}
