package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.response.FormExcelDownloadDTO;
import com.example.kodyjobdam.form.dto.response.FormFileArchiveDTO;
import com.example.kodyjobdam.form.entity.FormAnswerEntity;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormQuestionEntity;
import com.example.kodyjobdam.form.entity.FormSubmissionEntity;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.form.repository.FormSubmissionRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 선생님이 폼 응답을 엑셀로, 첨부 파일(포트폴리오)을 ZIP으로 내려받는다.
 *
 * <p>엑셀의 파일 칸에는 ZIP 안의 파일 이름을 적는다. 학생마다 같은 이름(포트폴리오.pdf 등)으로 올리는 일이 많아
 * 원래 이름 앞에 학번·이름을 붙여야 ZIP 안에서 겹치지 않고, 엑셀에서 찾은 이름으로 ZIP에서 바로 찾을 수 있다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormExportService {

    /** 엑셀 한 칸에 담을 수 있는 최대 글자 수 */
    private static final int MAX_CELL_LENGTH = 32_767;

    /** 파일 이름이 너무 길면 압축을 풀 때 경로 길이 제한에 걸린다. */
    private static final int MAX_FILE_NAME_LENGTH = 120;

    /** Windows에서 파일 이름에 쓸 수 없는 문자 */
    private static final String UNSAFE_FILE_NAME_CHARS = "\\/:*?\"<>|";

    private static final List<String> FIXED_HEADERS = List.of("학번", "이름");

    private final FormRepository formRepository;

    private final FormSubmissionRepository submissionRepository;

    private final FormFileStorage storage;

    /** 폼의 모든 응답을 한 행에 한 명씩 담은 엑셀 */
    @Transactional(readOnly = true)
    public FormExcelDownloadDTO exportExcel(Long formId, Long teacherId) {
        FormEntity form = findOwnedForm(formId, teacherId);
        List<FormSubmissionEntity> submissions = findSubmissions(formId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSheet(workbook, form, submissions, archiveNames(submissions));
            workbook.write(out);
            return new FormExcelDownloadDTO(safeFileName(form.getTitle()) + "_응답.xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("응답 엑셀을 만들지 못했습니다.", e);
        }
    }

    /** 폼에 첨부된 파일을 ZIP으로 묶을 목록. 파일 내용은 {@link #writeFileArchive}가 읽는다. */
    @Transactional(readOnly = true)
    public FormFileArchiveDTO prepareFileArchive(Long formId, Long teacherId) {
        FormEntity form = findOwnedForm(formId, teacherId);

        List<FormFileArchiveDTO.Entry> entries = archiveNames(findSubmissions(formId)).entrySet().stream()
                .map(entry -> new FormFileArchiveDTO.Entry(entry.getValue(), entry.getKey().getFile().getStoredName()))
                .toList();
        if (entries.isEmpty()) {
            throw FormException.notFound("내려받을 첨부 파일이 없습니다.");
        }

        return new FormFileArchiveDTO(safeFileName(form.getTitle()) + "_포트폴리오.zip", entries);
    }

    /**
     * 저장소의 파일을 하나씩 읽어 ZIP으로 바로 흘려 보낸다. 파일이 많아도 메모리에 한꺼번에 올리지 않는다.
     * 저장소에서 사라진 파일은 건너뛴다. 이미 응답을 보내기 시작해서 오류로 바꿀 수 없다.
     */
    public void writeFileArchive(FormFileArchiveDTO archive, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        // PDF·이미지는 이미 압축돼 있어 더 줄지 않는다. 시간을 덜 쓰는 쪽을 고른다.
        zip.setLevel(Deflater.BEST_SPEED);

        for (FormFileArchiveDTO.Entry entry : archive.entries()) {
            Resource resource;
            try {
                resource = storage.load(entry.storedName());
            } catch (FormException e) {
                log.warn("저장소에 없는 첨부 파일이라 ZIP에서 뺍니다. name={}, storedName={}",
                        entry.name(), entry.storedName());
                continue;
            }

            zip.putNextEntry(new ZipEntry(entry.name()));
            try (InputStream in = resource.getInputStream()) {
                in.transferTo(zip);
            }
            zip.closeEntry();
        }

        // 응답 스트림은 서블릿 컨테이너가 닫는다. 여기서는 ZIP 끝만 써 둔다.
        zip.finish();
        zip.flush();
    }

    private FormEntity findOwnedForm(Long formId, Long teacherId) {
        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> FormException.notFound("폼을 찾을 수 없습니다."));
        if (form.getUser() == null || !form.getUser().getId().equals(teacherId)) {
            throw FormException.forbidden("폼 제출 내역에 접근할 권한이 없습니다.");
        }
        return form;
    }

    /** 학번 순. 엑셀과 ZIP을 따로 내려받아도 파일 이름이 같게 나오도록 순서를 고정한다. */
    private List<FormSubmissionEntity> findSubmissions(Long formId) {
        return submissionRepository.findAllWithAnswersByFormId(formId).stream()
                .sorted(Comparator.comparing((FormSubmissionEntity submission) ->
                                Objects.toString(submission.getUser().getStudent_number(), ""))
                        .thenComparing(FormSubmissionEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * 파일 답변마다 ZIP 안에서 쓸 이름. "학번_이름_원래파일명" 꼴이다.
     * 이름이 겹치면 "(2)"처럼 번호를 붙인다. Windows는 대소문자를 가리지 않으므로 대소문자만 다른 이름도 겹친 것으로 본다.
     */
    private Map<FormAnswerEntity, String> archiveNames(List<FormSubmissionEntity> submissions) {
        Map<FormAnswerEntity, String> names = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();

        for (FormSubmissionEntity submission : submissions) {
            User user = submission.getUser();
            submission.getAnswers().stream()
                    .filter(answer -> answer.getFile() != null)
                    .sorted(Comparator.comparingInt(answer -> answer.getQuestion().getOrderIndex()))
                    .forEach(answer -> names.put(answer, uniqueName(safeFileName(
                            user.getStudent_number() + "_" + user.getName() + "_"
                                    + answer.getFile().getOriginalName()), used)));
        }
        return names;
    }

    private String uniqueName(String name, Set<String> used) {
        String candidate = name;
        for (int number = 2; !used.add(candidate.toLowerCase(Locale.ROOT)); number++) {
            candidate = withSuffix(name, " (" + number + ")");
        }
        return candidate;
    }

    private void writeSheet(Workbook workbook, FormEntity form, List<FormSubmissionEntity> submissions,
                            Map<FormAnswerEntity, String> fileNames) {
        Sheet sheet = workbook.createSheet("응답");
        List<FormQuestionEntity> questions = form.getQuestions().stream()
                .sorted(Comparator.comparingInt(FormQuestionEntity::getOrderIndex))
                .toList();

        List<String> headers = new ArrayList<>(FIXED_HEADERS);
        questions.forEach(question -> headers.add(question.getTitle()));
        writeHeader(workbook, sheet, headers);

        int rowIndex = 1;
        for (FormSubmissionEntity submission : submissions) {
            Row row = sheet.createRow(rowIndex++);
            User user = submission.getUser();
            setText(row, 0, user.getStudent_number());
            setText(row, 1, user.getName());

            // 질문 프록시를 초기화하지 않도록 id로 맞춘다.
            Map<Long, FormAnswerEntity> answersByQuestionId = submission.getAnswers().stream()
                    .collect(Collectors.toMap(answer -> answer.getQuestion().getId(), Function.identity(),
                            (first, second) -> first));
            for (int i = 0; i < questions.size(); i++) {
                FormQuestionEntity question = questions.get(i);
                FormAnswerEntity answer = answersByQuestionId.get(question.getId());
                if (answer != null) {
                    setText(row, FIXED_HEADERS.size() + i, answerText(question, answer, fileNames));
                }
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rowIndex - 1, 0), 0, headers.size() - 1));
        // 글꼴이 없는 서버(도커)에서는 autoSizeColumn이 실패할 수 있어 너비를 정해 둔다.
        for (int i = 0; i < headers.size(); i++) {
            int width = i < FIXED_HEADERS.size() ? 12 : 30;
            sheet.setColumnWidth(i, width * 256);
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet, List<String> headers) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(Objects.toString(headers.get(i), ""));
            cell.setCellStyle(style);
        }
    }

    private String answerText(FormQuestionEntity question, FormAnswerEntity answer,
                              Map<FormAnswerEntity, String> fileNames) {
        if (question.getType().isFile()) {
            return fileNames.get(answer);
        }
        if (question.getType().hasOptions()) {
            return answer.getSelectedOptions().stream()
                    .map(selected -> selected.getOption().getLabel())
                    .collect(Collectors.joining(", "));
        }
        return answer.getTextValue();
    }

    /** 문자열 칸. 학생이 "="로 시작하는 답을 써도 수식이 아닌 글자로 들어간다. */
    private void setText(Row row, int column, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        String text = value.length() > MAX_CELL_LENGTH ? value.substring(0, MAX_CELL_LENGTH - 1) + "…" : value;
        row.createCell(column).setCellValue(text);
    }

    /** Windows에서도 풀 수 있는 파일 이름으로 바꾼다. 확장자는 남기고 너무 긴 이름은 자른다. */
    private String safeFileName(String value) {
        // Mac에서 올린 파일 이름은 한글 자모가 풀린 꼴(NFD)이라 Windows에서 "ㅍㅗㅌㅡ…"처럼 보인다. 완성형으로 합친다.
        String normalized = Normalizer.normalize(Objects.toString(value, ""), Normalizer.Form.NFC).trim();
        StringBuilder name = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            name.append(Character.isISOControl(c) || UNSAFE_FILE_NAME_CHARS.indexOf(c) >= 0 ? '_' : c);
        }
        // Windows는 끝의 점·공백을 지워 버려 다른 파일과 겹칠 수 있다.
        String cleaned = name.toString().replaceAll("[. ]+$", "");
        if (cleaned.isEmpty()) {
            cleaned = "폼";
        }
        if (cleaned.length() <= MAX_FILE_NAME_LENGTH) {
            return cleaned;
        }

        int dot = cleaned.lastIndexOf('.');
        String extension = dot > 0 && cleaned.length() - dot <= 10 ? cleaned.substring(dot) : "";
        return cleaned.substring(0, MAX_FILE_NAME_LENGTH - extension.length()) + extension;
    }

    private String withSuffix(String name, String suffix) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) + suffix + name.substring(dot) : name + suffix;
    }
}
