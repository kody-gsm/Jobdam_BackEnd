package com.example.kodyjobdam.notice.service;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.CounselingPeriod;
import com.example.kodyjobdam.common.repository.CommonRepository;
import com.example.kodyjobdam.common.service.CounselingReservationCryptoService;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.repository.CourseRepository;
import com.example.kodyjobdam.notice.entity.CounselingReminder;
import com.example.kodyjobdam.notice.entity.ReservationKind;
import com.example.kodyjobdam.notice.repository.CounselingReminderRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CounselingReminderClaimService {

    private static final int CLAIM_LEASE_MINUTES = 10;

    private final CommonRepository commonRepository;
    private final CourseRepository courseRepository;
    private final CounselingReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final CounselingReservationCryptoService cryptoService;

    public List<Long> commonReservationIds(LocalDate date) {
        return commonRepository.findAllByStateAndDate(
                com.example.kodyjobdam.common.entity.StateEnum.RESERVED, date).stream()
                .map(CommonEntity::getReservation_id).toList();
    }

    public List<Long> courseReservationIds(LocalDate date) {
        return courseRepository.findAllByStateAndDate(
                com.example.kodyjobdam.course.entity.StateEnum.RESERVED, date).stream()
                .map(CourseEntity::getReservation_id).toList();
    }

    /** 예약 잠금과 발송 선점은 여기서 끝내고, 실제 Discord 호출은 트랜잭션 밖에서 한다. */
    @Transactional
    public Delivery claim(ReservationKind kind, Long reservationId, int hoursBefore, LocalDateTime now) {
        ReminderTarget target = findReservedTarget(kind, reservationId).orElse(null);
        if (target == null || target.encryptedUserId() == null) return null;

        CounselingPeriod period = CounselingPeriod.from(target.period()).orElse(null);
        if (period == null) return null;

        LocalDateTime startsAt = period.startsAt(target.date());
        LocalDateTime dueAt = startsAt.minusHours(hoursBefore);
        LocalDateTime cutoff = hoursBefore == 2 ? startsAt.minusHours(1) : startsAt;
        if (now.isBefore(dueAt) || !now.isBefore(cutoff)) return null;

        Long studentId = Long.valueOf(cryptoService.decrypt(target.encryptedUserId()));
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getDiscordUserId() == null
                || !cryptoService.submitterHash(studentId).equals(target.submitterHash())) return null;

        CounselingReminder reminder = reminderRepository.findBySlot(kind, reservationId, startsAt, hoursBefore)
                .orElse(null);
        if (reminder != null && (reminder.getSentAt() != null
                || (reminder.getClaimedAt() != null
                && now.isBefore(reminder.getClaimedAt().plusMinutes(CLAIM_LEASE_MINUTES))))) return null;

        if (reminder == null) reminder = new CounselingReminder(kind, reservationId, startsAt, hoursBefore);
        String token = UUID.randomUUID().toString();
        reminder.claim(token, now);
        reminder = reminderRepository.saveAndFlush(reminder);

        String category = kind == ReservationKind.COMMON ? "일반" : "진로";
        String prefix = now.isBefore(dueAt.plusMinutes(5))
                ? "상담 시작 " + hoursBefore + "시간 전입니다. " : "상담 일정 알림입니다. ";
        String message = prefix + target.date() + " " + target.period() + " " + category + " 상담 일정이 있습니다.";
        return new Delivery(reminder.getId(), token, student.getDiscordUserId(), message);
    }

    @Transactional
    public void markSent(Long reminderId, String token, LocalDateTime now) {
        reminderRepository.findByIdForUpdate(reminderId)
                .filter(reminder -> reminder.isClaimedBy(token))
                .ifPresent(reminder -> reminder.markSent(now));
    }

    @Transactional
    public void release(Long reminderId, String token) {
        reminderRepository.findByIdForUpdate(reminderId)
                .filter(reminder -> reminder.isClaimedBy(token))
                .ifPresent(CounselingReminder::release);
    }

    private Optional<ReminderTarget> findReservedTarget(ReservationKind kind, Long id) {
        if (kind == ReservationKind.COMMON) {
            return commonRepository.findByIdForUpdate(id)
                    .filter(entity -> entity.getState() == com.example.kodyjobdam.common.entity.StateEnum.RESERVED)
                    .map(entity -> new ReminderTarget(entity.getDate(), entity.getPeriod(),
                            entity.getSubmitterHash(), entity.getEncryptedUserId()));
        }
        return courseRepository.findByIdForUpdate(id)
                .filter(entity -> entity.getState() == com.example.kodyjobdam.course.entity.StateEnum.RESERVED)
                .map(entity -> new ReminderTarget(entity.getDate(), entity.getPeriod(),
                        entity.getSubmitterHash(), entity.getEncryptedUserId()));
    }

    public record Delivery(Long reminderId, String token, String discordUserId, String message) {
    }

    private record ReminderTarget(LocalDate date, String period, String submitterHash, String encryptedUserId) {
    }
}
