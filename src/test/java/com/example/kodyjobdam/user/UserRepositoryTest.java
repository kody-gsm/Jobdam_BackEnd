package com.example.kodyjobdam.user;

import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-null-student-number-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void 학번과_학적정보가_null인_학생을_저장하고_다시_읽는다() {
        User student = User.builder()
                .name("졸업생")
                .email("graduate@gsm.hs.kr")
                .role(UserRole.STUDENT)
                .build();
        entityManager.persist(student);
        entityManager.flush();
        entityManager.clear();

        User saved = entityManager.find(User.class, student.getId());
        assertThat(saved.getStudent_number()).isNull();
        assertThat(saved.getGrade()).isNull();
        assertThat(saved.getClassNum()).isNull();
        assertThat(saved.getNumber()).isNull();
    }
}
