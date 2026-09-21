package com.example.kodyjobdam.recruit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 채용 공고 이미지를 로그인 없이도 볼 수 있도록 디스크 경로를 정적 리소스로 공개한다. */
@Configuration
public class RecruitImageWebConfig implements WebMvcConfigurer {

    private final Path uploadDir;
    private final String publicPath;

    public RecruitImageWebConfig(
            @Value("${recruit.image.storage-path:./uploads/recruit}") String uploadDir,
            @Value("${recruit.image.public-path:/uploads/recruit}") String publicPath
    ) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicPath + "/**")
                .addResourceLocations(toResourceLocation(uploadDir));
        registry.addResourceHandler("/backend" + publicPath + "/**")
                .addResourceLocations(toResourceLocation(uploadDir));
    }

    private String normalizePublicPath(String publicPath) {
        String normalized = StringUtils.trimTrailingCharacter(publicPath, '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private String toResourceLocation(Path path) {
        String resourceLocation = path.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
        return resourceLocation;
    }
}
