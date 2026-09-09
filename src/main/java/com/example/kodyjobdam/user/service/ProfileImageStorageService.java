package com.example.kodyjobdam.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final Path uploadDir;
    private final String publicPath;
    private final DataSize maxFileSize;

    public ProfileImageStorageService(
            @Value("${app.profile-image.upload-dir:uploads/profile-images}") String uploadDir,
            @Value("${app.profile-image.public-path:/uploads/profile-images}") String publicPath,
            @Value("${app.profile-image.max-size:5MB}") DataSize maxFileSize
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
        this.maxFileSize = maxFileSize;
    }

    public StoredProfileImage store(MultipartFile image) {
        validate(image);

        try {
            Files.createDirectories(uploadDir);

            String extension = resolveExtension(image);
            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(fileName).normalize();

            if (!targetPath.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid profile image file name.");
            }

            image.transferTo(targetPath);

            return new StoredProfileImage(publicPath + "/" + fileName);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save profile image.", e);
        }
    }

    public void delete(String imageUrl) {
        try {
            deleteImage(imageUrl);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete profile image.", e);
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Profile image is required.");
        }

        String contentType = image.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only png, jpg, jpeg, and webp profile images are allowed.");
        }

        if (image.getSize() > maxFileSize.toBytes()) {
            throw new IllegalArgumentException("Profile image size must be less than or equal to " + maxFileSize.toMegabytes() + "MB.");
        }
    }

    private String resolveExtension(MultipartFile image) {
        String originalFilename = image.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            return switch (image.getContentType()) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
        }

        extension = "." + extension.toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".webp" -> extension;
            default -> throw new IllegalArgumentException("Only png, jpg, jpeg, and webp profile images are allowed.");
        };
    }

    private void deleteImage(String imageUrl) throws IOException {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith(publicPath + "/")) {
            return;
        }

        String fileName = imageUrl.substring((publicPath + "/").length());
        Path imagePath = uploadDir.resolve(fileName).normalize();
        if (imagePath.startsWith(uploadDir)) {
            Files.deleteIfExists(imagePath);
        }
    }

    private String normalizePublicPath(String publicPath) {
        String normalized = StringUtils.trimTrailingCharacter(publicPath, '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    public record StoredProfileImage(String url) {
    }
}
