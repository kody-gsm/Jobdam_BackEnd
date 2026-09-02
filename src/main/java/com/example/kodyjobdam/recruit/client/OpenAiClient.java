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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/responses";

    private static final List<String> SUPPORTED_MIME_TYPES =
            List.of("image/png", "image/jpeg", "image/gif", "image/webp");

    private static final String PROMPT = """
            다음 이미지는 채용 공고 또는 면접 안내문입니다.
            이미지에서 아래 항목을 추출하세요. 해당 정보가 없으면 null로 표기하세요.

            - companyName: 회사(기업) 이름
            - interviewDate: 면접 일자 및 시간
            - deadline: 지원서 접수/제출 마감 기한
            - summary: 그 외 지원자가 꼭 알아야 할 중요 정보(전형 절차, 준비물, 장소 등)를 2~3문장으로 요약
            """;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5.6}")
    private String model;

    public RecruitAnalysisResult analyze(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ConfigException.serviceUnavailable("OpenAI API key is not configured.");
        }

        if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw RecruitException.badRequest("지원하지 않는 이미지 형식입니다: " + mimeType);
        }

        String dataUrl = "data:" + mimeType + ";base64,"
                + Base64.getEncoder().encodeToString(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                    ENDPOINT, new HttpEntity<>(buildRequestBody(dataUrl), headers), String.class);
        } catch (RestClientException e) {
            log.error("OpenAI API 호출 실패 (model={})", model, e);
            throw RecruitException.badGateway("이미지 분석 요청에 실패했습니다.");
        }

        return parseResponse(response.getBody());
    }

    private ObjectNode buildRequestBody(String dataUrl) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        ObjectNode message = root.putArray("input").addObject();
        message.put("role", "user");

        ArrayNode content = message.putArray("content");
        content.addObject()
                .put("type", "input_text")
                .put("text", PROMPT);
        content.addObject()
                .put("type", "input_image")
                .put("image_url", dataUrl)
                .put("detail", "auto");

        // Structured Outputs: 스키마를 강제해 JSON 파싱 실패를 없앤다
        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "recruit_analysis");
        format.put("strict", true);
        format.set("schema", buildSchema());

        return root;
    }

    private ObjectNode buildSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        // strict 모드는 모든 필드를 required로 요구하므로, 없을 수 있는 값은 null 허용으로 표현한다
        for (String field : List.of("companyName", "interviewDate", "deadline", "summary")) {
            ArrayNode type = properties.putObject(field).putArray("type");
            type.add("string");
            type.add("null");
            required.add(field);
        }

        return schema;
    }

    private RecruitAnalysisResult parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            StringBuilder text = new StringBuilder();
            // output에는 reasoning 항목이 먼저 올 수 있어 message 항목을 찾아 훑는다
            for (JsonNode item : root.path("output")) {
                for (JsonNode part : item.path("content")) {
                    String type = part.path("type").asText();
                    if ("refusal".equals(type)) {
                        log.warn("OpenAI 분석 거부: {}", part.path("refusal").asText());
                        throw RecruitException.unprocessableEntity("이미지를 분석할 수 없습니다.");
                    }
                    if ("output_text".equals(type)) {
                        text.append(part.path("text").asText());
                    }
                }
            }

            if (text.isEmpty()) {
                throw RecruitException.unprocessableEntity("이미지에서 정보를 추출하지 못했습니다.");
            }

            return objectMapper.readValue(text.toString(), RecruitAnalysisResult.class);
        } catch (RecruitException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", body, e);
            throw RecruitException.internalServerError("분석 결과를 해석하지 못했습니다.");
        }
    }
}
