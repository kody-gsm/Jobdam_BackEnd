package com.example.kodyjobdam.recruit.dto.response;

import com.example.kodyjobdam.recruit.dto.RecruitPeriodDTO;
import com.example.kodyjobdam.recruit.entity.RecruitEntity;
import com.example.kodyjobdam.recruit.entity.RecruitPeriod;
import com.example.kodyjobdam.recruit.entity.RecruitStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecruitResponseDTO {

    private Long id;

    private String companyName;

    private RecruitPeriodDTO documentPeriod;

    private RecruitPeriodDTO writtenExamPeriod;

    private RecruitPeriodDTO practicalExamPeriod;

    private RecruitPeriodDTO codingTestPeriod;

    private RecruitPeriodDTO interviewPeriod;

    /** 지원 마감일(서류 접수 종료일). documentPeriod에서 뽑아낸 표기용 값이다. 공고에 없으면 "미정". */
    private String deadline;

    /** 면접 일정 표기용 값. interviewPeriod에서 뽑아낸다. 공고에 없으면 "미정". */
    private String interviewDate;

    private String summary;

    /** 공고 이미지 공개 URL. 로그인 없이도 접근 가능. 이미지가 없으면 null. */
    private String imageUrl;

    /** 공고와 함께 만들어진 지원 폼 식별자 */
    private Long formId;

    private RecruitStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static RecruitResponseDTO from(RecruitEntity entity) {
        return RecruitResponseDTO.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .documentPeriod(RecruitPeriodDTO.from(entity.getDocumentPeriod()))
                .writtenExamPeriod(RecruitPeriodDTO.from(entity.getWrittenExamPeriod()))
                .practicalExamPeriod(RecruitPeriodDTO.from(entity.getPracticalExamPeriod()))
                .codingTestPeriod(RecruitPeriodDTO.from(entity.getCodingTestPeriod()))
                .interviewPeriod(RecruitPeriodDTO.from(entity.getInterviewPeriod()))
                .deadline(orUndecided(entity.getDeadline()))
                .interviewDate(orUndecided(entity.getInterviewDate()))
                .summary(entity.getSummary())
                .imageUrl(entity.getImageUrl())
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 공고 이미지에서 일정을 읽지 못하면 화면에 "미정"으로 보여준다.
     *
     * <p>엔티티에는 채우지 않는다. 마감일은 알림 만료 계산에도 쓰이므로 날짜가 아닌 값이 섞이면 안 된다.</p>
     */
    private static String orUndecided(String value) {
        return value == null || value.isBlank() ? RecruitPeriod.UNDECIDED : value;
    }
}
