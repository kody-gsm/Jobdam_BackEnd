package com.example.kodyjobdam.common.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum CounselingCategoryEnum {
    ACADEMIC("학업"),
    EMPLOYMENT("취업"),
    ADMISSION("진학"),
    LIFE("생활"),
    ETC("기타");

    private final String label;

    CounselingCategoryEnum(String label) {
        this.label = label;
    }

    @JsonCreator
    public static CounselingCategoryEnum from(String value) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(value) || category.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 상담 분야입니다."));
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
