package com.example.kodyjobdam.recruit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesImageAndReturnsPublicUrl() throws Exception {
        RecruitImageStorage storage = new RecruitImageStorage(tempDir.toString(), "/uploads/recruit");

        String url = storage.store("test-image".getBytes(), "png");

        assertThat(url).startsWith("/uploads/recruit/");
        assertThat(url).endsWith(".png");
        String relativeName = url.substring("/uploads/recruit/".length());
        assertThat(Files.exists(tempDir.resolve(relativeName))).isTrue();
    }

    @Test
    void deleteRemovesStoredImage() throws Exception {
        RecruitImageStorage storage = new RecruitImageStorage(tempDir.toString(), "/uploads/recruit");
        String url = storage.store("test-image".getBytes(), "png");
        String relativeName = url.substring("/uploads/recruit/".length());

        storage.delete(url);

        assertThat(Files.exists(tempDir.resolve(relativeName))).isFalse();
    }

    @Test
    void deleteIgnoresNullOrForeignUrl() {
        RecruitImageStorage storage = new RecruitImageStorage(tempDir.toString(), "/uploads/recruit");

        storage.delete(null);
        storage.delete("/uploads/profile-images/other.png");
        // 예외 없이 조용히 넘어가면 성공이다.
    }
}
