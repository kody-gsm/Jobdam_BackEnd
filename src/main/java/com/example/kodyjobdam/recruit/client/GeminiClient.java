package com.example.kodyjobdam.recruit.client;

import com.example.kodyjobdam.common.exception.ConfigException;
import com.example.kodyjobdam.common.exception.RecruitException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private static final String PROMPT = """
            다음 이미지는 채용 공고 또는 면접 안내문입니다.
            이미지에서 아래 항목을 추출해서 JSON으로만 응답하세요. 해당 정보가 없으면 null로 표기하세요.

            - companyName: 회사(기업) 이름
            - interviewDate: 면접 일자 및 시간
            - deadline: 지원서 접수/제출 마감 기한
            - summary: 그 외 지원자가 꼭 알아야 할 중요 정보(전형 절차, 준비물, 장소 등)를 2~3문장으로 요약

            반드시 아래 JSON 형식으로만 응답하세요.
            {"companyName": string|null, "interviewDate": string|null, "deadline": string|null, "summary": string|null}
            """;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String model;

    public GeminiAnalysisResult analyze(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ConfigException.serviceUnavailable("Gemini API key is not configured.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        ObjectNode requestBody = buildRequestBody(base64Image, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        String url = String.format(ENDPOINT, model);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), String.class);
        } catch (RestClientException e) {
            log.error("Gemini API 호출 실패: {}", url, e);
            throw RecruitException.badGateway("이미지 분석 요청에 실패했습니다.");
        }

        return parseResponse(response.getBody());
    }

    private ObjectNode buildRequestBody(String base64Image, String mimeType) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode parts = root.putArray("contents").addObject().putArray("parts");
        parts.addObject().put("text", PROMPT);

        ObjectNode inlineData = parts.addObject().putObject("inlineData");
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Image);

        root.putObject("generationConfig").put("responseMimeType", "application/json");

        return root;
    }

    private GeminiAnalysisResult parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();

            if (text.isBlank()) {
                throw RecruitException.unprocessableEntity("이미지에서 정보를 추출하지 못했습니다.");
            }

            return objectMapper.readValue(text, GeminiAnalysisResult.class);
        } catch (RecruitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", body, e);
            throw RecruitException.internalServerError("분석 결과를 해석하지 못했습니다.");
        }
    }
}
