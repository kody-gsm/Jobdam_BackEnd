package com.example.kodyjobdam.recruit.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                // 이미지 한 장 분석이라 응답이 30초를 넘길 수 있다
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }
}
