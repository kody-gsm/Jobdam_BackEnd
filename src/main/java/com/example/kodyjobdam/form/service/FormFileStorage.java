package com.example.kodyjobdam.form.service;

import com.example.kodyjobdam.common.exception.FormException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 폼 첨부 파일을 서버 디스크에 저장한다.
 * 파일 이름은 서버가 UUID로 새로 만들고, 원래 이름은 DB에만 남긴다(경로 조작 방지).
 */
@Slf4j
@Component
public class FormFileStorage {

    private static final DateTimeFormatter SUB_DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    public FormFileStorage(@Value("${form.file.storage-path:./uploads/form}") String storagePath) {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createRootDirectory() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("폼 첨부 파일 저장 경로를 만들 수 없습니다: " + root, e);
        }
    }

    /** 파일을 저장하고 저장소 안에서의 상대 경로를 돌려준다 */
    public String store(MultipartFile file, String extension) {
        String storedName = LocalDate.now().format(SUB_DIRECTORY)
                + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw FormException.badRequest("파일을 저장하지 못했습니다.");
        }

        return storedName;
    }

    public Resource load(String storedName) {
        try {
            Resource resource = new UrlResource(resolve(storedName).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw FormException.notFound("파일이 저장소에 없습니다.");
            }
            return resource;
        } catch (IOException e) {
            throw FormException.notFound("파일을 읽을 수 없습니다.");
        }
    }

    /** 저장된 파일을 지운다. 이미 없으면 조용히 넘어간다. */
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException e) {
            log.warn("폼 첨부 파일 삭제 실패: {}", storedName, e);
        }
    }

    /** 저장소 바깥을 가리키는 경로를 막는다 */
    private Path resolve(String storedName) {
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw FormException.badRequest("잘못된 파일 경로입니다.");
        }
        return target;
    }
}
