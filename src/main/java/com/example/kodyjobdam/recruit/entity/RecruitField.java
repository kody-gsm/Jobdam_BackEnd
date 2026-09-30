package com.example.kodyjobdam.recruit.entity;

/**
 * 공고가 뽑는 직무 분야. 한 공고가 여러 직무를 함께 뽑을 수 있어 목록으로 다룬다.
 *
 * <p>공고 이미지를 분석할 때 AI가 고르고, 선생님이 검토 단계에서 고칠 수 있다.
 * 학생이 자기 관심 분야로 공고를 거를 때 쓴다.</p>
 *
 * <p>값을 추가할 때는 {@code GeminiClient}의 프롬프트 설명도 함께 늘려야 한다.
 * 설명이 없으면 AI가 그 값을 고르지 못한다.</p>
 */
public enum RecruitField {
    FRONTEND,   // 웹 프론트엔드
    BACKEND,    // 서버·API·DB
    FULLSTACK,  // 프론트엔드와 백엔드를 함께
    MOBILE,     // Android·iOS·Flutter 앱
    IOT,        // 임베디드·펌웨어·하드웨어 제어
    AI,         // 인공지능·머신러닝·데이터 분석
    SECURITY,   // 정보보안·보안 관제
    ETC         // 위 어디에도 넣기 어려운 직무
}
