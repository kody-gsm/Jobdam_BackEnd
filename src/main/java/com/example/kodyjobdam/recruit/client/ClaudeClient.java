package com.example.kodyjobdam.recruit.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import com.example.kodyjobdam.common.exception.ConfigException;
import com.example.kodyjobdam.common.exception.RecruitException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private static final String SYSTEM_PROMPT = """
            당신은 채용 공고 이미지에서 정보를 추출하는 도구입니다.
            설명이나 인사말 없이 JSON 객체 하나만 출력하세요. 코드 블록으로 감싸지 마세요.
            """;

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

    private final ObjectMapper objectMapper;

    @Value("${claude.api.key:}")
    private String apiKey;

    @Value("${claude.model:claude-opus-5}")
    private String model;

    /** 키가 없는 환경에서도 애플리케이션이 뜨도록 최초 호출 시점에 생성한다 */
    private volatile AnthropicClient client;

    public RecruitAnalysisResult analyze(byte[] imageBytes, String mimeType) {
        AnthropicClient anthropic = client();

        List<ContentBlockParam> content = List.of(
                ContentBlockParam.ofImage(ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .mediaType(mediaType(mimeType))
                                .data(Base64.getEncoder().encodeToString(imageBytes))
                                .build())
                        .build()),
                ContentBlockParam.ofText(TextBlockParam.builder().text(PROMPT).build())
        );

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(8192L)
                .system(SYSTEM_PROMPT)
                // 정형화된 추출 작업이라 낮은 effort로 충분하다
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.LOW)
                        .build())
                .addUserMessageOfBlockParams(content)
                .build();

        Message response;
        try {
            response = anthropic.messages().create(params);
        } catch (RateLimitException e) {
            log.error("Claude 호출 한도 초과", e);
            throw RecruitException.badGateway("이미지 분석 요청이 몰려 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
        } catch (AnthropicServiceException e) {
            log.error("Claude API 호출 실패 (model={})", model, e);
            throw RecruitException.badGateway("이미지 분석 요청에 실패했습니다.");
        }

        return parseResponse(response);
    }

    private AnthropicClient client() {
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw ConfigException.serviceUnavailable("Claude API key is not configured.");
                    }
                    local = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                    client = local;
                }
            }
        }
        return local;
    }

    /** Claude가 지원하는 이미지 형식만 통과시킨다 (JPEG, PNG, GIF, WebP) */
    private Base64ImageSource.MediaType mediaType(String mimeType) {
        return switch (mimeType == null ? "" : mimeType) {
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/jpeg" -> Base64ImageSource.MediaType.IMAGE_JPEG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> throw RecruitException.badRequest("지원하지 않는 이미지 형식입니다: " + mimeType);
        };
    }

    private RecruitAnalysisResult parseResponse(Message response) {
        // 안전 거부(refusal)나 빈 응답이면 본문이 비어 있다
        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", String::concat)
                .trim();

        if (text.isBlank()) {
            throw RecruitException.unprocessableEntity("이미지에서 정보를 추출하지 못했습니다.");
        }

        String json = stripCodeFence(text);

        try {
            return objectMapper.readValue(json, RecruitAnalysisResult.class);
        } catch (Exception e) {
            log.error("Claude 응답 파싱 실패: {}", text, e);
            throw RecruitException.internalServerError("분석 결과를 해석하지 못했습니다.");
        }
    }

    /** 모델이 ```json ... ``` 으로 감싸 응답하는 경우를 대비한다 */
    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int start = trimmed.indexOf('\n');
        int end = trimmed.lastIndexOf("```");
        if (start < 0 || end <= start) {
            return trimmed;
        }
        return trimmed.substring(start + 1, end).trim();
    }
}
