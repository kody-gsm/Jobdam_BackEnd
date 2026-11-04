package com.example.kodyjobdam.passreport.repository;

import com.example.kodyjobdam.passreport.entity.PassReportEntity;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
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

    private static final LocalDateTime NOV_1 = LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final LocalDateTime NOV_2 = LocalDateTime.of(2026, 11, 2, 9, 0);
    private static final LocalDateTime NOV_3 = LocalDateTime.of(2026, 11, 3, 9, 0);

    @Autowired
    private PassReportRepository passReportRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 학생은_내가_보낸_보고만_최근에_제출한_것부터_읽는다() {
        User student = saveUser("3101", "홍길동");
        User otherStudent = saveUser("2101", "김철수");

        PassReportEntity naver = save(student, "네이버", NOV_3);
        PassReportEntity kakao = save(student, "카카오", NOV_1);
        PassReportEntity line = save(student, "라인", NOV_1);
        save(otherStudent, "토스", NOV_2);
        entityManager.flush();
        entityManager.clear();

        List<PassReportEntity> sent = passReportRepository.findByStudent_IdOrderBySubmittedAtDescIdDesc(student.getId());

        // 같은 시각에 제출했으면 나중에 저장한 보고가 먼저 나온다.
        assertThat(sent).extracting(PassReportEntity::getId)
                .containsExactly(naver.getId(), line.getId(), kakao.getId());
        assertThat(sent.get(0).getSubmittedAt()).isEqualTo(NOV_3);
    }

    @Test
    void 선생님은_전체_보고를_학번순으로_읽고_한_학생의_보고는_먼저_제출한_것부터_읽는다() {
        User student = saveUser("3101", "홍길동");
        User otherStudent = saveUser("2101", "김철수");

        PassReportEntity naver = save(student, "네이버", NOV_3);
        PassReportEntity toss = save(otherStudent, "토스", NOV_2);
        PassReportEntity kakao = save(student, "카카오", NOV_1);
        entityManager.flush();
        entityManager.clear();

        List<PassReportEntity> all = passReportRepository.findAllByOrderByStudentNumberAscSubmittedAtAscIdAsc();

        assertThat(all).extracting(PassReportEntity::getId)
                .containsExactly(toss.getId(), kakao.getId(), naver.getId());
        assertThat(all).extracting(PassReportEntity::getStudentNumber).containsExactly("2101", "3101", "3101");
        assertThat(all).extracting(PassReportEntity::getStudentName).containsExactly("김철수", "홍길동", "홍길동");
        assertThat(all).extracting(PassReportEntity::getSubmittedAt).containsExactly(NOV_2, NOV_1, NOV_3);
    }

    private PassReportEntity save(User student, String companyName, LocalDateTime submittedAt) {
        PassReportEntity report = PassReportEntity.builder()
                .student(student)
                .studentNumber(student.getStudent_number())
                .studentName(student.getName())
                .companyName(companyName)
                .submittedAt(submittedAt)
                .build();
        entityManager.persist(report);
        return report;
    }

    private User saveUser(String studentNumber, String name) {
        User user = User.builder()
                .name(name)
                .student_number(studentNumber)
                .email(studentNumber + "@test.com")
                .role(UserRole.STUDENT)
                .emailVerified(true)
                .build();
        entityManager.persist(user);
        return user;
    }
}
