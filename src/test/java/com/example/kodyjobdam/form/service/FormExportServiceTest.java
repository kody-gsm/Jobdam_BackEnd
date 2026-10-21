package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.response.FormExcelDownloadDTO;
import com.example.kodyjobdam.form.dto.response.FormFileArchiveDTO;
import com.example.kodyjobdam.form.entity.FormAnswerEntity;
import com.example.kodyjobdam.form.entity.FormAnswerOptionEntity;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormQuestionOptionEntity;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.entity.SubmissionStatus;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormExportServiceTest {

    private static final Long TEACHER_ID = 1L;

    @Mock
    private FormRepository formRepository;

    @Mock
    private FormSubmissionRepository submissionRepository;

    @Mock
    private FormFileStorage storage;

    @InjectMocks
    private FormExportService formExportService;

    private FormEntity form;
    private FormQuestionEntity motive;
    private FormQuestionEntity field;
    private FormQuestionEntity portfolio;
    private FormQuestionOptionEntity backend;
    private FormQuestionOptionEntity frontend;

    @BeforeEach
    void setUp() {
        motive = question(11L, 1, QuestionType.LONG_TEXT, "지원 동기");
        field = question(12L, 2, QuestionType.MULTIPLE_CHOICE, "희망 분야");
        portfolio = question(13L, 3, QuestionType.FILE, "포트폴리오");
        backend = FormQuestionOptionEntity.builder().id(21L).orderIndex(1).label("백엔드").build();
        frontend = FormQuestionOptionEntity.builder().id(22L).orderIndex(2).label("프론트엔드").build();
        form = FormEntity.builder()
                .id(5L)
                .user(user(TEACHER_ID, "선생님", "T001", UserRole.TEACHER))
                .title("카카오/네이버 채용: 1차")
                .questions(new ArrayList<>(List.of(portfolio, motive, field)))
                .build();
    }

    @Test
    void 응답_엑셀은_학번순으로_한_명씩_담고_파일_칸에는_ZIP_안의_파일_이름을_적는다() throws IOException {
        // Mac에서 올린 파일은 한글 자모가 풀린 이름(NFD)으로 들어온다.
        String macFileName = Normalizer.normalize("포트폴리오.pdf", Normalizer.Form.NFD);
        FormSubmissionEntity later = submission(31L, user(3L, "김철수", "3102", UserRole.STUDENT),
                SubmissionStatus.SUBMITTED,
                textAnswer(motive, "=1+1 로 시작하는 답"),
                fileAnswer(portfolio, file(macFileName, "2026/10/b.pdf")));
        FormSubmissionEntity first = submission(30L, user(2L, "홍길동", "3101", UserRole.STUDENT),
                SubmissionStatus.CONFIRMED,
                textAnswer(motive, "백엔드가 좋아서"),
                choiceAnswer(field, backend, frontend),
                fileAnswer(portfolio, file("포트폴리오.pdf", "2026/10/a.pdf")));
        givenSubmissions(later, first);

        FormExcelDownloadDTO excel = formExportService.exportExcel(5L, TEACHER_ID);

        assertThat(excel.fileName()).isEqualTo("카카오_네이버 채용_ 1차_응답.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.content()))) {
            Sheet sheet = workbook.getSheet("응답");
            assertThat(texts(sheet.getRow(0), 6))
                    .containsExactly("학번", "이름", "지원 동기", "희망 분야", "포트폴리오", "");
            assertThat(texts(sheet.getRow(1), 5)).containsExactly(
                    "3101", "홍길동", "백엔드가 좋아서", "백엔드, 프론트엔드", "3101_홍길동_포트폴리오.pdf");
            // 파일 이름은 완성형(NFC)으로 합쳐져 Windows에서도 한글이 제대로 보인다.
            assertThat(texts(sheet.getRow(2), 5)).containsExactly(
                    "3102", "김철수", "=1+1 로 시작하는 답", "", "3102_김철수_포트폴리오.pdf");
            // 학생 답이 수식으로 계산되지 않고 글자 그대로 들어간다.
            assertThat(sheet.getRow(2).getCell(2).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void 첨부_파일은_엑셀과_같은_이름으로_ZIP에_담고_저장소에_없는_파일은_건너뛴다() throws IOException {
        FormSubmissionEntity submission = submission(30L, user(2L, "홍길동", "3101", UserRole.STUDENT),
                SubmissionStatus.SUBMITTED,
                fileAnswer(portfolio, file("포트폴리오.pdf", "2026/10/a.pdf")));
        FormQuestionEntity resume = question(14L, 4, QuestionType.FILE, "이력서");
        form.getQuestions().add(resume);
        submission.getAnswers().add(fileAnswer(resume, file("포트폴리오.PDF", "2026/10/c.pdf")));
        FormSubmissionEntity missing = submission(31L, user(3L, "김철수", "3102", UserRole.STUDENT),
                SubmissionStatus.SUBMITTED,
                fileAnswer(portfolio, file("작품.zip", "2026/10/gone.zip")));
        givenSubmissions(submission, missing);
        when(storage.load("2026/10/a.pdf")).thenReturn(new ByteArrayResource("A".getBytes()));
        when(storage.load("2026/10/c.pdf")).thenReturn(new ByteArrayResource("C".getBytes()));
        when(storage.load("2026/10/gone.zip")).thenThrow(FormException.notFound("파일이 저장소에 없습니다."));

        FormFileArchiveDTO archive = formExportService.prepareFileArchive(5L, TEACHER_ID);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        formExportService.writeFileArchive(archive, out);

        assertThat(archive.fileName()).isEqualTo("카카오_네이버 채용_ 1차_포트폴리오.zip");
        // 대소문자만 다른 이름도 Windows에서는 겹치므로 번호를 붙인다.
        assertThat(unzip(out.toByteArray())).containsExactly(
                Map.entry("3101_홍길동_포트폴리오.pdf", "A"),
                Map.entry("3101_홍길동_포트폴리오 (2).PDF", "C"));
    }

    @Test
    void 첨부_파일이_하나도_없으면_ZIP을_만들지_않는다() {
        givenSubmissions(submission(30L, user(2L, "홍길동", "3101", UserRole.STUDENT),
                SubmissionStatus.SUBMITTED, textAnswer(motive, "답변")));

        assertThatThrownBy(() -> formExportService.prepareFileArchive(5L, TEACHER_ID))
                .isInstanceOf(FormException.class)
                .hasMessage("내려받을 첨부 파일이 없습니다.");
    }

    @Test
    void 폼을_만든_선생님이_아니면_내려받을_수_없다() {
        when(formRepository.findById(5L)).thenReturn(Optional.of(form));

        assertThatThrownBy(() -> formExportService.exportExcel(5L, 99L))
                .isInstanceOf(FormException.class)
                .hasMessage("폼 제출 내역에 접근할 권한이 없습니다.");
        verify(submissionRepository, never()).findAllWithAnswersByFormId(5L);
    }

    private void givenSubmissions(FormSubmissionEntity... submissions) {
        when(formRepository.findById(5L)).thenReturn(Optional.of(form));
        when(submissionRepository.findAllWithAnswersByFormId(5L)).thenReturn(List.of(submissions));
    }

    /** 엑셀에 보이는 그대로의 글자. 빈 칸은 빈 문자열이다. */
    private List<String> texts(Row row, int columns) {
        DataFormatter formatter = new DataFormatter();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            values.add(formatter.formatCellValue(row.getCell(i)));
        }
        return values;
    }

    private Map<String, String> unzip(byte[] zip) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private FormQuestionEntity question(Long id, int orderIndex, QuestionType type, String title) {
        return FormQuestionEntity.builder().id(id).orderIndex(orderIndex).type(type).title(title).build();
    }

    private FormSubmissionEntity submission(Long id, User student, SubmissionStatus status,
                                            FormAnswerEntity... answers) {
        return FormSubmissionEntity.builder()
                .id(id)
                .form(form)
                .user(student)
                .status(status)
                .submittedAt(LocalDateTime.of(2026, 10, 16, 9, 30))
                .answers(new ArrayList<>(List.of(answers)))
                .build();
    }

    private FormAnswerEntity textAnswer(FormQuestionEntity question, String text) {
        return FormAnswerEntity.builder().question(question).textValue(text).build();
    }

    private FormAnswerEntity choiceAnswer(FormQuestionEntity question, FormQuestionOptionEntity... options) {
        FormAnswerEntity answer = FormAnswerEntity.builder().question(question).build();
        for (FormQuestionOptionEntity option : options) {
            answer.addSelectedOption(FormAnswerOptionEntity.builder().option(option).build());
        }
        return answer;
    }

    private FormAnswerEntity fileAnswer(FormQuestionEntity question, FormFileEntity file) {
        return FormAnswerEntity.builder().question(question).file(file).build();
    }

    private FormFileEntity file(String originalName, String storedName) {
        return FormFileEntity.builder().originalName(originalName).storedName(storedName).build();
    }

    private User user(Long id, String name, String studentNumber, UserRole role) {
        return User.builder()
                .id(id)
                .name(name)
                .student_number(studentNumber)
                .email("user" + id + "@test.com")
                .role(role)
                .emailVerified(true)
                .build();
    }
}
