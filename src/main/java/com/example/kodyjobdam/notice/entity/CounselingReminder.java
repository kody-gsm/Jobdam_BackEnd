package com.example.kodyjobdam.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "counseling_reminder", uniqueConstraints = @UniqueConstraint(
        columnNames = {"reservation_kind", "reservation_id", "starts_at", "hours_before"}))
public class CounselingReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_kind", nullable = false, length = 16)
    private ReservationKind reservationKind;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "hours_before", nullable = false)
    private int hoursBefore;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "discord_user_id", length = 32)
    private String discordUserId;

    @Column(name = "discord_message_id", length = 32)
    private String discordMessageId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    public CounselingReminder(ReservationKind reservationKind, Long reservationId,
                              LocalDateTime startsAt, int hoursBefore) {
        this.reservationKind = reservationKind;
        this.reservationId = reservationId;
        this.startsAt = startsAt;
        this.hoursBefore = hoursBefore;
    }

    public void claim(String token, LocalDateTime at) {
        this.claimToken = token;
        this.claimedAt = at;
    }

    public boolean isClaimedBy(String token) {
        return token != null && token.equals(claimToken);
    }

    public void markSent(LocalDateTime at, String discordUserId, String discordMessageId) {
        this.sentAt = at;
        this.discordUserId = discordUserId;
        this.discordMessageId = discordMessageId;
        release();
    }

    public void markDeleted(LocalDateTime at) {
        this.deletedAt = at;
    }

    public void release() {
        this.claimToken = null;
        this.claimedAt = null;
    }
}
