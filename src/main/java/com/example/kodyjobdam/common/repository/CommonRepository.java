package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
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
public interface CommonRepository extends JpaRepository<CommonEntity, Long> {

    List<CommonEntity> findAllByDateAndPeriod(LocalDate date, String period);

    List<CommonEntity> findAllByDateAndPeriodAndTeacher_Id(LocalDate date, String period, Long teacherId);

    /** 예약이 걸린 시간만 읽는다. 엔티티로 읽지 않아야 뒤이은 잠금 조회가 최신 상태를 가져온다. */
    @Query("select new com.example.kodyjobdam.common.repository.ReservationSlot(c.date, c.period, c.teacher.id) "
            + "from CommonEntity c where c.reservation_id = :reservationId")
    Optional<ReservationSlot> findSlotByReservationId(@Param("reservationId") Long reservationId);

    /** 같은 선생님·같은 시간의 예약을 잠가서 읽는다. 동시에 들어온 수락은 먼저 잡은 쪽이 끝날 때까지 기다린다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommonEntity c "
            + "where c.date = :date and c.period = :period and c.teacher.id = :teacherId "
            + "order by c.reservation_id")
    List<CommonEntity> findAllForUpdateByDateAndPeriodAndTeacherId(@Param("date") LocalDate date,
                                                                @Param("period") String period,
                                                                @Param("teacherId") Long teacherId);

    /** 날짜가 지난 신청을 잠가서 읽는다. 같은 신청을 선생님이 동시에 수락해도 한쪽만 반영된다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommonEntity c where c.state = :state and c.date < :date order by c.reservation_id")
    List<CommonEntity> findAllForUpdateByStateAndDateBefore(@Param("state") StateEnum state,
                                                          @Param("date") LocalDate date);

    boolean existsBySubmitterHashAndDateAndPeriodAndStateNot(String submitterHash, LocalDate date, String period,
                                                            StateEnum state);

    /** 학생이 이 시간에 취소하지 않은 상담 신청을 갖고 있는지 */
    default boolean existsActiveReservation(String submitterHash, LocalDate date, String period) {
        return existsBySubmitterHashAndDateAndPeriodAndStateNot(submitterHash, date, period, StateEnum.CANCEL);
    }

    @Query("select c.period from CommonEntity c "
            + "where c.date = :date and c.submitterHash = :submitterHash and c.state <> :excluded")
    List<String> findPeriodsBySubmitterHashAndDateAndStateNot(@Param("date") LocalDate date,
                                                             @Param("submitterHash") String submitterHash,
                                                             @Param("excluded") StateEnum excluded);

    /** 학생이 그날 신청해 둔 교시. 선생님이 달라도 같은 시간에는 다시 신청할 수 없다. */
    default List<String> findActivePeriods(String submitterHash, LocalDate date) {
        return findPeriodsBySubmitterHashAndDateAndStateNot(date, submitterHash, StateEnum.CANCEL);
    }

    List<CommonEntity> findAllByDateInAndTeacher_IdIn(Collection<LocalDate> dates, Collection<Long> teacherIds);

    /** 매주 반복 잠금을 걸 때, 앞으로 이 교시에 잡혀 있는 신청을 요일별로 걸러 취소하기 위해 읽는다. */
    List<CommonEntity> findAllByPeriodAndTeacher_IdAndDateGreaterThanEqual(
            String period, Long teacherId, LocalDate from);

    List<CommonEntity> findAllByDateAndTeacher_IdOrderByPeriodAsc(LocalDate date, Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findBySubmitterHash(String submitterHash);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findByTeacher_Id(Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<CommonEntity> findByTeacher_IdAndStateOrderByDateAscPeriodAsc(Long teacherId, StateEnum state);
}
