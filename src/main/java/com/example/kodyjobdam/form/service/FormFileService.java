package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import com.example.kodyjobdam.form.dto.response.FormFileDownloadDTO;
import com.example.kodyjobdam.form.dto.response.FormFileResponseDTO;
import com.example.kodyjobdam.form.entity.FormEntity;
import com.example.kodyjobdam.form.entity.FormFileEntity;
import com.example.kodyjobdam.form.entity.QuestionType;
import com.example.kodyjobdam.form.repository.FormFileRepository;
import com.example.kodyjobdam.form.repository.FormRepository;
import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/** 폼 답변에 첨부할 파일을 올리고 내려받는다 */
@Service
@RequiredArgsConstructor
public class FormFileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FormFileRepository fileRepository;

    private final FormRepository formRepository;

    private final UserRepository userRepository;

    private final FormFileStorage storage;

    @Value("${form.file.allowed-extensions:pdf,png,jpg,jpeg,webp,gif,zip,doc,docx,ppt,pptx,hwp,hwpx,txt,md}")
    private List<String> allowedExtensions;

    /** 학생: 답변에 붙일 파일 업로드. 제출할 때 여기서 받은 id를 답변의 fileId로 보낸다. */
    @Transactional
    public FormFileResponseDTO upload(Long formId, MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw FormException.badRequest("파일을 첨부해주세요.");
        }

        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> FormException.notFound("폼을 찾을 수 없습니다."));
        if (!form.isAcceptingSubmission()) {
            throw FormException.badRequest("지금은 응답을 받지 않는 폼입니다.");
        }
        if (form.getQuestions().stream().noneMatch(question -> question.getType() == QuestionType.FILE)) {
            throw FormException.badRequest("이 폼에는 파일을 첨부할 질문이 없습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> FormException.notFound("회원이 없습니다."));

        String originalName = sanitizeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw FormException.badRequest("올릴 수 없는 형식입니다. 가능한 형식: " + String.join(", ", allowedExtensions));
        }

        FormFileEntity saved = fileRepository.save(FormFileEntity.builder()
                .form(form)
                .user(user)
                .originalName(originalName)
                .storedName(storage.store(file, extension))
                .contentType(file.getContentType() == null ? DEFAULT_CONTENT_TYPE : file.getContentType())
                .size(file.getSize())
                .build());

        return FormFileResponseDTO.from(saved);
    }

    /** 파일을 올린 학생 본인과 폼을 만든 선생님만 내려받을 수 있다 */
    @Transactional(readOnly = true)
    public FormFileDownloadDTO download(Long fileId, Long userId) {
        FormFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> FormException.notFound("파일을 찾을 수 없습니다."));
        validateReadable(file, userId);

        return new FormFileDownloadDTO(
                storage.load(file.getStoredName()),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize());
    }

    private void validateReadable(FormFileEntity file, Long userId) {
        boolean uploader = file.getUser() != null && file.getUser().getId().equals(userId);
        User formOwner = file.getForm() == null ? null : file.getForm().getUser();
        boolean formTeacher = formOwner != null && formOwner.getId().equals(userId);

        if (!uploader && !formTeacher) {
            throw FormException.forbidden("이 파일을 볼 권한이 없습니다.");
        }
    }

    /** 폼이 지워질 때 그 폼에 올라온 파일도 함께 정리한다 */
    @Transactional
    public void deleteAllByForm(Long formId) {
        List<FormFileEntity> files = fileRepository.findByFormId(formId);
        if (files.isEmpty()) {
            return;
        }

        files.forEach(file -> storage.delete(file.getStoredName()));
        fileRepository.deleteAll(files);
    }

    /** 경로가 섞여 들어와도 파일 이름만 남긴다 */
    private String sanitizeName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw FormException.badRequest("파일 이름을 읽을 수 없습니다.");
        }

        String name = Paths.get(originalFilename.replace('\\', '/')).getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw FormException.badRequest("파일 이름을 읽을 수 없습니다.");
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw FormException.badRequest("확장자가 있는 파일만 올릴 수 있습니다.");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
