package com.example.kodyjobdam.recruit.client;

import com.example.kodyjobdam.common.exception.BusinessException;
import com.example.kodyjobdam.common.exception.ConfigException;
import com.example.kodyjobdam.common.exception.RecruitException;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            - documentPeriod: 서류 접수 기간. "지원 기간", "모집 기간", "접수 기간", "지원 마감"은 모두 여기에 넣으세요.
            - writtenExamPeriod: 필기 전형 기간
            - practicalExamPeriod: 실기 전형 기간
            - codingTestPeriod: 코딩테스트 전형 기간
            - interviewPeriod: 면접 전형 기간. "면접 일정", "면접일"도 여기에 넣으세요.
            - summary: 그 외 지원자가 꼭 알아야 할 중요 정보(준비물, 장소, 상세 시각 등)를 2~3문장으로 요약

            기간 항목은 {"startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD"} 형태의 객체로만 작성하세요.
            - 날짜는 반드시 YYYY-MM-DD 형식이어야 하며 다른 형식이나 설명을 덧붙이지 마세요.
            - 하루만 진행하는 전형은 startDate와 endDate에 같은 날짜를 넣으세요.
            - 연도가 적혀 있지 않으면 가장 가까운 미래 연도로 추정하세요.
            - 공고에 없는 전형은 그 항목 전체를 null로 두세요.
            - 시작일 없이 마감일만 적혀 있으면 startDate는 null로 두고 endDate에 마감일을 넣으세요.
            - 시각(예: 14:00)은 기간에 넣지 말고 summary에 적으세요.

            반드시 아래 JSON 형식으로만 응답하세요.
            {"companyName": string|null, "documentPeriod": object|null, "writtenExamPeriod": object|null, "practicalExamPeriod": object|null, "codingTestPeriod": object|null, "interviewPeriod": object|null, "summary": string|null}
            """;

    /** 연-월-일 사이에 무엇이 끼어 있어도 숫자만 뽑아낸다. */
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\D{1,3}(\\d{1,2})\\D{1,3}(\\d{1,2})");

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
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                log.warn("Gemini API 호출량 제한 초과: {}", e.getResponseBodyAsString());
                throw RecruitException.tooManyRequests(
                        "이미지 분석 요청량이 한도를 넘었습니다. 잠시 후 다시 시도해주세요.");
            }
            log.error("Gemini API 호출 실패: url={}, status={}, body={}",
                    url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw RecruitException.badGateway("이미지 분석 요청에 실패했습니다.");
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
            String text = extractText(root);

            if (text.isBlank()) {
                throw RecruitException.unprocessableEntity("이미지에서 정보를 추출하지 못했습니다.");
            }

            JsonNode data = objectMapper.readTree(text);

            GeminiAnalysisResult result = new GeminiAnalysisResult(
                    readText(data, "companyName"),
                    readPeriod(data, "documentPeriod"),
                    readPeriod(data, "writtenExamPeriod"),
                    readPeriod(data, "practicalExamPeriod"),
                    readPeriod(data, "codingTestPeriod"),
                    readPeriod(data, "interviewPeriod"),
                    readText(data, "summary")
            );

            if (result.documentPeriod() == null || result.interviewPeriod() == null) {
                log.warn("Gemini가 서류 접수/면접 기간을 채우지 않았습니다: model={}, 응답={}", model, text);
            }

            return result;
        } catch (RecruitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", body, e);
            throw RecruitException.internalServerError("분석 결과를 해석하지 못했습니다.");
        }
    }

    /** 추론 과정 조각은 건너뛰고 응답 본문 조각만 이어붙인다. */
    private String extractText(JsonNode root) {
        StringBuilder text = new StringBuilder();
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            text.append(part.path("text").asText(""));
        }
        return text.toString().trim();
    }

    /** 날짜 형식이 어긋난 항목은 버리고 나머지는 살린다. */
    private RecruitPeriod readPeriod(JsonNode data, String fieldName) {
        JsonNode node = data.path(fieldName);
        if (node.isTextual()) {
            return readTextPeriod(fieldName, node.asText());
        }
        if (!node.isObject()) {
            return null;
        }

        LocalDate startDate = readDate(node, "startDate");
        LocalDate endDate = readDate(node, "endDate");
        if (startDate == null && endDate == null) {
            return null;
        }

        return new RecruitPeriod(startDate, endDate);
    }

    /** 객체 대신 "2026-09-01 ~ 2026-09-10"처럼 문자열로 돌아온 기간도 받아준다. */
    private RecruitPeriod readTextPeriod(String fieldName, String value) {
        try {
            return RecruitPeriod.parse(value);
        } catch (BusinessException e) {
            log.warn("Gemini가 해석할 수 없는 기간을 반환했습니다: {}={}", fieldName, value);
            return null;
        }
    }

    private LocalDate readDate(JsonNode node, String fieldName) {
        String value = readText(node, fieldName);
        if (value == null) {
            return null;
        }

        LocalDate date = parseDate(value);
        if (date == null) {
            log.warn("Gemini가 해석할 수 없는 날짜를 반환했습니다: {}={}", fieldName, value);
        }
        return date;
    }

    /**
     * "2026-09-10" 외에 "2026.09.10", "2026년 9월 10일", 뒤에 시각이나 요일이 붙은 값도 받아준다.
     * 형식만 어긋났을 뿐 날짜가 적혀 있는 응답을 통째로 버리지 않기 위한 처리다.
     */
    private LocalDate parseDate(String value) {
        Matcher matcher = DATE_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }

        try {
            return LocalDate.of(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }
}
