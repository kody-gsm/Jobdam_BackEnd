package com.example.kodyjobdam.form.entity;

public enum QuestionType {
    SHORT_TEXT,      // 단답형
    LONG_TEXT,       // 장문형
    SINGLE_CHOICE,   // 객관식 (하나만 선택)
    MULTIPLE_CHOICE, // 체크박스 (여러 개 선택)
    DROPDOWN,        // 드롭다운 (하나만 선택)
    NUMBER,          // 숫자
    DATE;            // 날짜 (YYYY-MM-DD)

    /** 선택지를 갖는 유형인지 */
    public boolean hasOptions() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE || this == DROPDOWN;
    }

    /** 선택지를 여러 개 고를 수 있는 유형인지 */
    public boolean allowsMultiple() {
        return this == MULTIPLE_CHOICE;
    }
}
