package com.example.kodyjobdam.recruit.client;

/** Gemini에 함께 넘겨 분석할 공고 이미지 한 장 */
public record GeminiImage(byte[] data, String mimeType) {
}
