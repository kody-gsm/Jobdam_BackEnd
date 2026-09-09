package com.example.kodyjobdam.recruit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        geminiClient = new GeminiClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(geminiClient, "apiKey", "test-key");
        ReflectionTestUtils.setField(geminiClient, "model", "gemini-3.1-flash-lite");
    }

    @Test
    void readsPeriodReturnedAsPlainString() {
        stubResponse("""
                {"companyName":"잡담",
                 "documentPeriod":"2026-09-01 ~ 2026-09-10",
                 "interviewPeriod":"2026-09-20",
                 "summary":"준비물은 신분증"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(new byte[]{1}, "image/png");

        assertThat(result.documentPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.documentPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(result.interviewPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.interviewPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void skipsThoughtPartsWhenReadingResponse() {
        String responseBody = """
                {"candidates":[{"content":{"parts":[
                    {"thought":true,"text":"공고를 살펴보자"},
                    {"text":"{\\"companyName\\":\\"잡담\\",\\"documentPeriod\\":{\\"startDate\\":\\"2026-09-01\\",\\"endDate\\":\\"2026-09-10\\"}}"}
                ]}}]}
                """;
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        GeminiAnalysisResult result = geminiClient.analyze(new byte[]{1}, "image/png");

        assertThat(result.companyName()).isEqualTo("잡담");
        assertThat(result.documentPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void dropsPeriodWithUnparsableText() {
        stubResponse("{\"documentPeriod\":\"9월 초 마감\",\"companyName\":\"잡담\"}");

        GeminiAnalysisResult result = geminiClient.analyze(new byte[]{1}, "image/png");

        assertThat(result.documentPeriod()).isNull();
        assertThat(result.companyName()).isEqualTo("잡담");
    }

    @Test
    void readsDatesWrittenInNonIsoFormats() {
        stubResponse("{\"documentPeriod\":{\"startDate\":\"2026.09.01\",\"endDate\":\"2026년 9월 10일\"},"
                + "\"interviewPeriod\":{\"startDate\":\"2026-09-20 14:00\",\"endDate\":\"2026/09/20(금)\"}}");

        GeminiAnalysisResult result = geminiClient.analyze(new byte[]{1}, "image/png");

        assertThat(result.documentPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.documentPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(result.interviewPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.interviewPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void dropsDateWithoutYear() {
        stubResponse("{\"documentPeriod\":{\"startDate\":\"9월 1일\",\"endDate\":\"9월 10일\"}}");

        GeminiAnalysisResult result = geminiClient.analyze(new byte[]{1}, "image/png");

        assertThat(result.documentPeriod()).isNull();
    }

    /** 모델이 돌려준 JSON 문자열을 Gemini 응답 형태로 감싼다. */
    private void stubResponse(String modelText) {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody;
        try {
            responseBody = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode().set("candidates", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode().set("content", objectMapper.createObjectNode()
                                    .set("parts", objectMapper.createArrayNode()
                                            .add(objectMapper.createObjectNode().put("text", modelText)))))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));
    }
}
