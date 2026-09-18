package com.example.kodyjobdam.recruit.service;

import com.example.kodyjobdam.common.exception.RecruitException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 채용 공고 이미지를 서버 디스크에 저장하고, 로그인 없이도 볼 수 있는 공개 URL을 돌려준다.
 * recruit 도메인 전용 저장소로, 다른 기능(form 등)에 의존하지 않는다.
 * 파일 이름은 서버가 UUID로 새로 만든다(경로 조작 방지).
 */
@Slf4j
@Component
public class RecruitImageStorage {

    private static final DateTimeFormatter SUB_DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    private final String publicPath;

    public RecruitImageStorage(
            @Value("${recruit.image.storage-path:./uploads/recruit}") String storagePath,
            @Value("${recruit.image.public-path:/uploads/recruit}") String publicPath) {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
    }

    @PostConstruct
    void createRootDirectory() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("채용 공고 이미지 저장 경로를 만들 수 없습니다: " + root, e);
        }
    }

    /** 이미지 바이트를 저장하고 누구나 접근할 수 있는 공개 URL을 돌려준다 */
    public String store(byte[] image, String extension) {
        String relativeName = LocalDate.now().format(SUB_DIRECTORY)
                + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = resolve(relativeName);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, image);
        } catch (IOException e) {
            throw RecruitException.badRequest("이미지를 저장하지 못했습니다.");
        }

        return publicPath + "/" + relativeName;
    }

    /** 저장된 이미지를 지운다. URL이 없거나 이미 지워졌으면 조용히 넘어간다. */
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(publicPath + "/")) {
            return;
        }

        String relativeName = imageUrl.substring((publicPath + "/").length());
        try {
            Files.deleteIfExists(resolve(relativeName));
        } catch (IOException e) {
            log.warn("채용 공고 이미지 삭제 실패: {}", imageUrl, e);
        }
    }

    /** 저장소 바깥을 가리키는 경로를 막는다 */
    private Path resolve(String relativeName) {
        Path target = root.resolve(relativeName).normalize();
        if (!target.startsWith(root)) {
            throw RecruitException.badRequest("잘못된 이미지 경로입니다.");
        }
        return target;
    }

    private String normalizePublicPath(String publicPath) {
        String normalized = StringUtils.trimTrailingCharacter(publicPath, '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
