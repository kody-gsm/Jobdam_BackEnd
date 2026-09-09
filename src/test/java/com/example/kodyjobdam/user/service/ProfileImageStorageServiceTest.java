package com.example.kodyjobdam.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesProfileImageAndReturnsPublicUrl() throws Exception {
        ProfileImageStorageService storageService = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-images",
                DataSize.ofMegabytes(5)
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "test-image".getBytes()
        );

        ProfileImageStorageService.StoredProfileImage storedImage = storageService.store(image);

        assertThat(storedImage.url()).startsWith("/uploads/profile-images/");
        String fileName = storedImage.url().substring("/uploads/profile-images/".length());
        assertThat(Files.exists(tempDir.resolve(fileName))).isTrue();
    }

    @Test
    void deleteRemovesStoredProfileImage() throws Exception {
        ProfileImageStorageService storageService = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-images",
                DataSize.ofMegabytes(5)
        );
        Path profileImage = Files.writeString(tempDir.resolve("profile.png"), "test-image");

        storageService.delete("/uploads/profile-images/profile.png");

        assertThat(Files.exists(profileImage)).isFalse();
    }

    @Test
    void storeRejectsUnsupportedContentType() {
        ProfileImageStorageService storageService = new ProfileImageStorageService(
                tempDir.toString(),
                "/uploads/profile-images",
                DataSize.ofMegabytes(5)
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.gif",
                "image/gif",
                "test-image".getBytes()
        );

        assertThatThrownBy(() -> storageService.store(image))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only png, jpg, jpeg, and webp profile images are allowed.");
    }
}
