package com.example.kodyjobdam.form.entity;

public enum FormStatus {
    DRAFT,      // 선생님이 작성 중인 초안 (학생에게 보이지 않음)
    PUBLISHED,  // 공개됨 (학생이 응답 제출 가능)
    CLOSED      // 마감됨 (더 이상 제출 불가)
}
