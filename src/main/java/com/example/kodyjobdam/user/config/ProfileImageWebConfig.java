package com.example.kodyjobdam.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class ProfileImageWebConfig implements WebMvcConfigurer {

    private final Path uploadDir;
    private final String publicPath;

    public ProfileImageWebConfig(
            @Value("${app.profile-image.upload-dir:uploads/profile-images}") String uploadDir,
            @Value("${app.profile-image.public-path:/uploads/profile-images}") String publicPath
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
