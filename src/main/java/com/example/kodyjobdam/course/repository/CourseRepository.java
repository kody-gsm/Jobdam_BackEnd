package com.example.kodyjobdam.course.repository;

import com.example.kodyjobdam.common.repository.ReservationSlot;
import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {

    List<CourseEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CourseEntity> findAllByDateAndPeriodAndTeacher_Id(LocalDate date, String period, Long teacherId);

    /** 예약이 걸린 시간만 읽는다. 엔티티로 읽지 않아야 뒤이은 잠금 조회가 최신 상태를 가져온다. */
    @Query("select new com.example.kodyjobdam.common.repository.ReservationSlot(c.date, c.period, c.teacher.id) "
            + "from CourseEntity c where c.reservation_id = :reservationId")
    Optional<ReservationSlot> findSlotByReservationId(@Param("reservationId") Long reservationId);

    /** 같은 선생님·같은 시간의 예약을 잠가서 읽는다. 동시에 들어온 수락은 먼저 잡은 쪽이 끝날 때까지 기다린다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseEntity c "
            + "where c.date = :date and c.period = :period and c.teacher.id = :teacherId "
            + "order by c.reservation_id")
    List<CourseEntity> findAllForUpdateByDateAndPeriodAndTeacherId(@Param("date") LocalDate date,
                                                                @Param("period") String period,
                                                                @Param("teacherId") Long teacherId);

    boolean existsBySubmitterHashAndDateAndPeriodAndStateNot(String submitterHash, LocalDate date, String period,
                                                            StateEnum state);

    /** 학생이 이 시간에 취소하지 않은 상담 신청을 갖고 있는지 */
    default boolean existsActiveReservation(String submitterHash, LocalDate date, String period) {
        return existsBySubmitterHashAndDateAndPeriodAndStateNot(submitterHash, date, period, StateEnum.CANCEL);
    }

    List<CourseEntity> findAllByDateInAndTeacher_IdIn(Collection<LocalDate> dates, Collection<Long> teacherIds);

    List<CourseEntity> findAllByDateAndTeacher_IdOrderByPeriodAsc(LocalDate date, Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CourseEntity> findBySubmitterHash(String submitterHash);

    @EntityGraph(attributePaths = {"teacher"})
    List<CourseEntity> findByTeacher_Id(Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CourseEntity> findByTeacher_IdAndStateOrderByDateAscPeriodAsc(Long teacherId, StateEnum state);
}
