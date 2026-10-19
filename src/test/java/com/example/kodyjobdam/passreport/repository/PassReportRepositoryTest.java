package com.example.kodyjobdam.passreport.repository;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import com.example.kodyjobdam.passreport.entity.PassType;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pass-report-repository-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PassReportRepositoryTest {

    @Autowired
    private PassReportRepository passReportRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 학생과_선생님별로_최신순으로_읽고_학생_선생님을_함께_가져온다() {
        User student = saveUser("student@test.com", UserRole.STUDENT);
        User otherStudent = saveUser("other@test.com", UserRole.STUDENT);
        User teacher = saveUser("teacher@test.com", UserRole.TEACHER);
        User otherTeacher = saveUser("teacher2@test.com", UserRole.TEACHER);

        PassReportEntity document = save(student, teacher, PassType.DOCUMENT, LocalDate.of(2026, 11, 2));
        PassReportEntity interview = save(student, teacher, PassType.INTERVIEW, null);
        save(otherStudent, otherTeacher, PassType.INTERVIEW, null);
        entityManager.flush();
        entityManager.clear();

        List<PassReportEntity> received = passReportRepository.findByTeacher_IdOrderByCreatedAtDescIdDesc(teacher.getId());
        List<PassReportEntity> sent = passReportRepository.findByStudent_IdOrderByCreatedAtDescIdDesc(student.getId());

        // 같은 시각에 들어오면 나중에 저장한 보고가 먼저 나온다.
        assertThat(received).extracting(PassReportEntity::getId).containsExactly(interview.getId(), document.getId());
        assertThat(sent).extracting(PassReportEntity::getId).containsExactly(interview.getId(), document.getId());
        assertThat(received.get(1).getInterviewDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(received.get(1).getCreatedAt()).isNotNull();
        assertThat(Hibernate.isInitialized(received.get(0).getStudent())).isTrue();
        assertThat(Hibernate.isInitialized(received.get(0).getTeacher())).isTrue();
    }

    private PassReportEntity save(User student, User teacher, PassType type, LocalDate interviewDate) {
        PassReportEntity report = PassReportEntity.builder()
                .student(student)
                .teacher(teacher)
                .type(type)
                .companyName("카카오")
                .companyPhone("02-123-4567")
                .interviewDate(interviewDate)
                .build();
        entityManager.persist(report);
        return report;
    }

    private User saveUser(String email, UserRole role) {
        User user = User.builder()
                .name("사용자")
                .student_number(email)
                .email(email)
                .role(role)
                .emailVerified(true)
                .build();
        entityManager.persist(user);
        return user;
    }
}
