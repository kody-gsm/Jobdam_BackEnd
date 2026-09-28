package com.example.kodyjobdam.banner.service;

import com.example.kodyjobdam.common.exception.BannerException;
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

@Slf4j
@Component
public class BannerImageStorage {

    private static final DateTimeFormatter SUB_DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    private final String publicPath;

    public BannerImageStorage(
            @Value("${banner.image.storage-path:./uploads/banner}") String storagePath,
            @Value("${banner.image.public-path:/uploads/banner}") String publicPath) {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
    }

    @PostConstruct
    void createRootDirectory() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("배너 이미지 저장 경로를 만들 수 없습니다: " + root, e);
        }
    }

    public String store(byte[] image, String extension) {
        String relativeName = LocalDate.now().format(SUB_DIRECTORY)
                + "/" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path target = resolve(relativeName);

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, image);
        } catch (IOException e) {
            throw BannerException.badRequest("배너 이미지를 저장하지 못했습니다.");
        }

        return publicPath + "/" + relativeName;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(publicPath + "/")) {
            return;
        }

        String relativeName = imageUrl.substring((publicPath + "/").length());
        try {
            Files.deleteIfExists(resolve(relativeName));
        } catch (IOException e) {
            log.warn("배너 이미지 삭제 실패: {}", imageUrl, e);
        }
    }

    private Path resolve(String relativeName) {
        Path target = root.resolve(relativeName).normalize();
        if (!target.startsWith(root)) {
            throw BannerException.badRequest("잘못된 배너 이미지 경로입니다.");
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
