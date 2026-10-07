package com.example.kodyjobdam.notice.repository;

import com.example.kodyjobdam.notice.entity.CounselingReminder;
import com.example.kodyjobdam.notice.entity.ReservationKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CounselingReminderRepository extends JpaRepository<CounselingReminder, Long> {

    @Query("select r from CounselingReminder r where r.reservationKind = :kind "
            + "and r.reservationId = :reservationId and r.startsAt = :startsAt and r.hoursBefore = :hoursBefore")
    Optional<CounselingReminder> findBySlot(
            @Param("kind") ReservationKind reservationKind, @Param("reservationId") Long reservationId,
            @Param("startsAt") LocalDateTime startsAt, @Param("hoursBefore") int hoursBefore);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CounselingReminder r where r.id = :id")
    Optional<CounselingReminder> findByIdForUpdate(@Param("id") Long id);
}
