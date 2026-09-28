package com.example.kodyjobdam.banner.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BannerImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storeSavesImageAndReturnsPublicUrl() throws Exception {
        BannerImageStorage storage = new BannerImageStorage(tempDir.toString(), "/uploads/banner");

        String url = storage.store("test-banner".getBytes(), "png");

        assertThat(url).startsWith("/uploads/banner/");
        assertThat(url).endsWith(".png");
        String relativeName = url.substring("/uploads/banner/".length());
        assertThat(Files.exists(tempDir.resolve(relativeName))).isTrue();
    }

    @Test
    void deleteRemovesStoredImage() throws Exception {
        BannerImageStorage storage = new BannerImageStorage(tempDir.toString(), "/uploads/banner");
        String url = storage.store("test-banner".getBytes(), "png");
        String relativeName = url.substring("/uploads/banner/".length());

        storage.delete(url);

        assertThat(Files.exists(tempDir.resolve(relativeName))).isFalse();
    }

    @Test
    void deleteIgnoresNullOrForeignUrl() {
        BannerImageStorage storage = new BannerImageStorage(tempDir.toString(), "/uploads/banner");

        storage.delete(null);
        storage.delete("/uploads/recruit/other.png");
    }
}
