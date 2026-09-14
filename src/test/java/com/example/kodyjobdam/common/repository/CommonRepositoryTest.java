package com.example.kodyjobdam.common.repository;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:common-repository-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommonRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 10);

    @Autowired
    private CommonRepository commonRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findSlotByReservationIdReadsDatePeriodAndTeacher() {
        User teacher = saveUser("teacher@test.com");
        CommonEntity reservation = save(teacher, "3교시", "student-hash", StateEnum.WAITING);
        entityManager.flush();
        entityManager.clear();

        assertThat(commonRepository.findSlotByReservationId(reservation.getReservation_id()))
                .contains(new ReservationSlot(DATE, "3교시", teacher.getId()));
    }

    @Test
    void findAllForUpdateReadsOnlySameTeacherAndTime() {
        User teacher = saveUser("teacher@test.com");
        User otherTeacher = saveUser("other@test.com");
        CommonEntity first = save(teacher, "3교시", "a", StateEnum.WAITING);
        CommonEntity second = save(teacher, "3교시", "b", StateEnum.RESERVED);
        save(teacher, "4교시", "c", StateEnum.WAITING);
        save(otherTeacher, "3교시", "d", StateEnum.WAITING);
        entityManager.flush();
        entityManager.clear();

        assertThat(commonRepository.findAllForUpdateByDateAndPeriodAndTeacherId(DATE, "3교시", teacher.getId()))
                .extracting(CommonEntity::getReservation_id)
                .containsExactly(first.getReservation_id(), second.getReservation_id());
    }

    private CommonEntity save(User teacher, String period, String submitterHash, StateEnum state) {
        CommonEntity entity = CommonEntity.builder()
                .teacher(teacher)
                .date(DATE)
                .period(period)
                .submitterHash(submitterHash)
                .state(state)
                .build();
        entityManager.persist(entity);
        return entity;
    }

    private User saveUser(String email) {
        User user = User.builder()
                .name("선생님")
                .student_number(email)
                .email(email)
                .role(UserRole.WEE_TEACHER)
                .emailVerified(true)
                .build();
        entityManager.persist(user);
        return user;
    }
}
