package com.example.kodyjobdam.passreport.entity;

/** 학생이 알리는 합격 단계 */
public enum PassType {

    /** 서류 합격. 다음 단계인 면접 날짜를 함께 알린다. */
    DOCUMENT("서류"),

    /** 면접 합격 */
    INTERVIEW("면접");

    private final String label;

    PassType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 면접 날짜를 받아야 하는 단계인지 */
    public boolean requiresInterviewDate() {
        return this == DOCUMENT;
    }
}
