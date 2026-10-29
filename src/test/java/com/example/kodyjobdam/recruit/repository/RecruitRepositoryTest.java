package com.example.kodyjobdam.recruit.repository;

import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recruit-repository-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecruitRepositoryTest {

    @Autowired
    private RecruitRepository recruitRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 공고_이미지는_올린_순서대로_읽고_뗀_이미지는_행도_지운다() {
        RecruitEntity recruit = RecruitEntity.builder().user(saveTeacher()).companyName("잡담").build();
        recruit.addImage("/uploads/recruit/2026/10/a.png");
        recruit.addImage("/uploads/recruit/2026/10/b.png");
        recruit.addImage("/uploads/recruit/2026/10/c.png");
        recruitRepository.saveAndFlush(recruit);
        entityManager.clear();

        RecruitEntity loaded = recruitRepository.findById(recruit.getId()).orElseThrow();
        assertThat(loaded.getImageUrls()).containsExactly(
                "/uploads/recruit/2026/10/a.png",
                "/uploads/recruit/2026/10/b.png",
                "/uploads/recruit/2026/10/c.png");

        loaded.removeImage(loaded.getImages().get(1).getId());
        recruitRepository.flush();
        entityManager.clear();

        RecruitEntity reloaded = recruitRepository.findById(recruit.getId()).orElseThrow();
        assertThat(reloaded.getImageUrls()).containsExactly(
                "/uploads/recruit/2026/10/a.png",
                "/uploads/recruit/2026/10/c.png");
        assertThat(entityManager.createQuery("select count(i) from RecruitImageEntity i", Long.class)
                .getSingleResult()).isEqualTo(2L);
    }

    private User saveTeacher() {
        User teacher = User.builder()
                .name("선생님")
                .student_number("teacher@test.com")
                .email("teacher@test.com")
                .role(UserRole.TEACHER)
                .emailVerified(true)
                .build();
        entityManager.persist(teacher);
        return teacher;
    }
}
