package com.example.kodyjobdam.form.repository;

import com.example.kodyjobdam.form.entity.FormAnswerEntity;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:form-submission-repository-test;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FormSubmissionRepositoryTest {

    @Autowired
    private FormSubmissionRepository submissionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 내려받기용_조회는_학생과_답변_첨부_파일을_중복_없이_함께_읽는다() {
        User teacher = saveUser("teacher@test.com", UserRole.TEACHER);
        FormEntity form = saveForm(teacher);
        FormEntity otherForm = saveForm(teacher);
        FormQuestionEntity motive = form.getQuestions().get(0);
        FormQuestionEntity portfolio = form.getQuestions().get(1);

        User student = saveUser("student@test.com", UserRole.STUDENT);
        FormFileEntity file = FormFileEntity.builder()
                .form(form).user(student).originalName("포트폴리오.pdf").storedName("2026/10/a.pdf").build();
        entityManager.persist(file);
        FormSubmissionEntity submission = FormSubmissionEntity.builder().form(form).user(student).build();
        submission.addAnswer(FormAnswerEntity.builder().question(motive).textValue("지원 동기").build());
        submission.addAnswer(FormAnswerEntity.builder().question(portfolio).file(file).build());
        entityManager.persist(submission);

        User otherStudent = saveUser("other@test.com", UserRole.STUDENT);
        entityManager.persist(FormSubmissionEntity.builder().form(form).user(otherStudent).build());
        entityManager.persist(FormSubmissionEntity.builder().form(otherForm).user(student).build());
        entityManager.flush();
        entityManager.clear();

        List<FormSubmissionEntity> found = submissionRepository.findAllWithAnswersByFormId(form.getId());

        assertThat(found).hasSize(2);
        FormSubmissionEntity loaded = found.stream()
                .filter(each -> each.getId().equals(submission.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(Hibernate.isInitialized(loaded.getUser())).isTrue();
        assertThat(Hibernate.isInitialized(loaded.getAnswers())).isTrue();
        assertThat(loaded.getAnswers()).hasSize(2);
        assertThat(loaded.getAnswers())
                .filteredOn(answer -> answer.getFile() != null)
                .singleElement()
                .satisfies(answer -> {
                    assertThat(Hibernate.isInitialized(answer.getFile())).isTrue();
                    assertThat(answer.getFile().getOriginalName()).isEqualTo("포트폴리오.pdf");
                });
    }

    private FormEntity saveForm(User teacher) {
        FormEntity form = FormEntity.builder().user(teacher).title("채용 지원서").build();
        form.addQuestion(FormQuestionEntity.builder().orderIndex(1).type(QuestionType.LONG_TEXT).title("지원 동기").build());
        form.addQuestion(FormQuestionEntity.builder().orderIndex(2).type(QuestionType.FILE).title("포트폴리오").build());
        entityManager.persist(form);
        return form;
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
