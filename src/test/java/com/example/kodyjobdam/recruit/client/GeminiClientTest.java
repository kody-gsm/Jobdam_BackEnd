package com.example.kodyjobdam.recruit.client;

import com.example.kodyjobdam.recruit.entity.RecruitField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiClientTest {

    private static final List<GeminiImage> IMAGE = List.of(new GeminiImage(new byte[]{1}, "image/png"));

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
    @SuppressWarnings("unchecked")
    void 여러_장을_보내면_이미지마다_붙여_한_번에_요청한다() {
        stubResponse("{\"companyName\":\"잡담\"}");

        geminiClient.analyze(List.of(
                new GeminiImage(new byte[]{1}, "image/png"),
                new GeminiImage(new byte[]{2}, "image/jpeg")));

        ArgumentCaptor<HttpEntity<JsonNode>> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), request.capture(), eq(String.class));
        JsonNode parts = request.getValue().getBody().path("contents").path(0).path("parts");
        assertThat(parts).hasSize(3);
        assertThat(parts.path(0).has("text")).isTrue();
        assertThat(parts.path(1).path("inlineData").path("mimeType").asText()).isEqualTo("image/png");
        assertThat(parts.path(1).path("inlineData").path("data").asText()).isEqualTo("AQ==");
        assertThat(parts.path(2).path("inlineData").path("mimeType").asText()).isEqualTo("image/jpeg");
        assertThat(parts.path(2).path("inlineData").path("data").asText()).isEqualTo("Ag==");
    }

    @Test
    void 직무_분야를_배열로_읽는다() {
        stubResponse("""
                {"companyName":"잡담",
                 "fields":["FRONTEND","BACKEND"],
                 "summary":"요약"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.fields()).containsExactly(RecruitField.FRONTEND, RecruitField.BACKEND);
    }

    @Test
    void 직무_분야가_문자열_하나로_와도_읽는다() {
        stubResponse("""
                {"companyName":"잡담","fields":"iot","summary":"요약"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.fields()).containsExactly(RecruitField.IOT);
    }

    @Test
    void 알_수_없는_직무_분야는_버리고_나머지를_살린다() {
        stubResponse("""
                {"companyName":"잡담","fields":["BACKEND","데브옵스",""],"summary":"요약"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.fields()).containsExactly(RecruitField.BACKEND);
    }

    @Test
    void 직무_분야가_없으면_추측하지_않고_비워둔다() {
        stubResponse("""
                {"companyName":"잡담","summary":"요약"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.fields()).isEmpty();
    }

    @Test
    void readsPeriodReturnedAsPlainString() {
        stubResponse("""
                {"companyName":"잡담",
                 "documentPeriod":"2026-09-01 ~ 2026-09-10",
                 "interviewPeriod":"2026-09-20",
                 "summary":"준비물은 신분증"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

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

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.companyName()).isEqualTo("잡담");
        assertThat(result.documentPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void dropsPeriodWithUnparsableText() {
        stubResponse("{\"documentPeriod\":\"9월 초 마감\",\"companyName\":\"잡담\"}");

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.documentPeriod()).isNull();
        assertThat(result.companyName()).isEqualTo("잡담");
    }

    @Test
    void readsDatesWrittenInNonIsoFormats() {
        stubResponse("{\"documentPeriod\":{\"startDate\":\"2026.09.01\",\"endDate\":\"2026년 9월 10일\"},"
                + "\"interviewPeriod\":{\"startDate\":\"2026-09-20 14:00\",\"endDate\":\"2026/09/20(금)\"}}");

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.documentPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.documentPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(result.interviewPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(result.interviewPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void dropsDateWithoutYear() {
        stubResponse("{\"documentPeriod\":{\"startDate\":\"9월 1일\",\"endDate\":\"9월 10일\"}}");

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.documentPeriod()).isNull();
    }

    @Test
    void dropsPeriodsWhenImageIsNotRecruitment() {
        stubResponse("""
                {"recruitment":false,
                 "companyName":"testify",
                 "documentPeriod":{"startDate":null,"endDate":"2026-09-29"},
                 "interviewPeriod":null,
                 "summary":"QA과정 교육생 모집\\n신청 마감: 2026-09-29 17:00\\n교육 일정: 2026-09-21 ~ 2026-11-19"}
                """);

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.documentPeriod()).isNull();
        assertThat(result.interviewPeriod()).isNull();
        assertThat(result.summary()).contains("신청 마감: 2026-09-29 17:00");
    }

    @Test
    void cutsSummaryLongerThanColumn() {
        stubResponse("{\"summary\":\"" + "가".repeat(1200) + "\"}");

        GeminiAnalysisResult result = geminiClient.analyze(IMAGE);

        assertThat(result.summary()).hasSize(1000);
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
