package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.response.FormFileResponseDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormStatus;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormFileRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormFileServiceTest {

    @Mock
    private FormFileRepository fileRepository;

    @Mock
    private FormRepository formRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FormFileStorage storage;

    @InjectMocks
    private FormFileService formFileService;

    @BeforeEach
    void setAllowedExtensions() {
        ReflectionTestUtils.setField(formFileService, "allowedExtensions", List.of("pdf", "png"));
    }

    @Test
    void 파일_질문이_있는_공개_폼에는_파일을_올릴_수_있다() {
        FormEntity form = publishedFormWithFileQuestion();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(userRepository.findById(3L)).thenReturn(Optional.of(student(3L)));
        when(storage.store(any(), eq("pdf"))).thenReturn("2026/09/uuid.pdf");
        when(fileRepository.save(any(FormFileEntity.class))).thenAnswer(returnsFirstArg());

        FormFileResponseDTO response = formFileService.upload(1L, pdf("portfolio.pdf"), 3L);

        assertThat(response.getOriginalName()).isEqualTo("portfolio.pdf");
        assertThat(response.getSize()).isEqualTo(4);
    }

    @Test
    void 허용하지_않는_확장자는_올릴_수_없다() {
        when(formRepository.findById(1L)).thenReturn(Optional.of(publishedFormWithFileQuestion()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(student(3L)));

        assertThatThrownBy(() -> formFileService.upload(1L, pdf("portfolio.exe"), 3L))
                .isInstanceOf(FormException.class)
                .hasMessageContaining("올릴 수 없는 형식입니다.");
    }

    @Test
    void 파일_질문이_없는_폼에는_올릴_수_없다() {
        FormEntity form = FormEntity.builder()
                .id(1L)
                .title("설문")
                .user(teacher(2L))
                .status(FormStatus.PUBLISHED)
                .build();
        form.addQuestion(FormQuestionEntity.builder()
                .id(10L)
                .orderIndex(1)
                .type(QuestionType.SHORT_TEXT)
                .title("이름")
                .build());
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formFileService.upload(1L, pdf("portfolio.pdf"), 3L))
                .isInstanceOf(FormException.class)
                .hasMessage("이 폼에는 파일을 첨부할 질문이 없습니다.");
    }

    @Test
    void 파일_이름에_경로가_섞여_있으면_이름만_남긴다() {
        FormEntity form = publishedFormWithFileQuestion();
        when(formRepository.findById(1L)).thenReturn(Optional.of(form));
        when(userRepository.findById(3L)).thenReturn(Optional.of(student(3L)));
        when(storage.store(any(), eq("pdf"))).thenReturn("2026/09/uuid.pdf");
        when(fileRepository.save(any(FormFileEntity.class))).thenAnswer(returnsFirstArg());

        FormFileResponseDTO response = formFileService.upload(1L, pdf("../../etc/portfolio.pdf"), 3L);

        assertThat(response.getOriginalName()).isEqualTo("portfolio.pdf");
    }

    @Test
    void 폼을_만든_선생님은_지원자_파일을_내려받을_수_있다() {
        when(fileRepository.findById(50L)).thenReturn(Optional.of(uploadedFile()));

        formFileService.download(50L, 2L);

        // 권한 검사를 통과하면 저장소에서 파일을 읽는다
        org.mockito.Mockito.verify(storage).load("2026/09/uuid.pdf");
    }

    @Test
    void 관계없는_사용자는_파일을_내려받을_수_없다() {
        when(fileRepository.findById(50L)).thenReturn(Optional.of(uploadedFile()));

        assertThatThrownBy(() -> formFileService.download(50L, 9L))
                .isInstanceOf(FormException.class)
                .hasMessage("이 파일을 볼 권한이 없습니다.");
    }

    private FormFileEntity uploadedFile() {
        return FormFileEntity.builder()
                .id(50L)
                .form(publishedFormWithFileQuestion())
                .user(student(3L))
                .originalName("portfolio.pdf")
                .storedName("2026/09/uuid.pdf")
                .contentType("application/pdf")
                .size(1024L)
                .build();
    }

    private FormEntity publishedFormWithFileQuestion() {
        FormEntity form = FormEntity.builder()
                .id(1L)
                .title("지원서")
                .user(teacher(2L))
                .status(FormStatus.PUBLISHED)
                .build();
        form.addQuestion(FormQuestionEntity.builder()
                .id(20L)
                .orderIndex(1)
                .type(QuestionType.FILE)
                .title("포트폴리오")
                .build());
        return form;
    }

    private MockMultipartFile pdf(String fileName) {
        return new MockMultipartFile("file", fileName, "application/pdf", new byte[]{1, 2, 3, 4});
    }

    private User teacher(Long id) {
        return User.builder().id(id).name("선생님").role(UserRole.TEACHER).build();
    }

    private User student(Long id) {
        return User.builder().id(id).name("학생").role(UserRole.STUDENT).build();
    }
}
