package com.example.kodyjobdam.passreport.dto.request;

import com.example.kodyjobdam.passreport.entity.PassType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 학생이 보내는 합격 보고. 보내는 학생은 토큰에서 정한다.
 *
 * <p>면접 날짜는 서류 합격(DOCUMENT)일 때만 받는다. 면접 합격(INTERVIEW)이면 보내도 저장하지 않는다.</p>
 */
@Getter
public class PassReportCreateDTO {

    @NotNull(message = "보고할 선생님을 선택해주세요.")
    private Long teacherId;

    @NotNull(message = "서류 합격인지 면접 합격인지 선택해주세요.")
    private PassType type;

    @NotBlank(message = "회사 이름을 입력해주세요.")
    @Size(max = 100, message = "회사 이름은 100자 이하로 입력해주세요.")
    private String companyName;

    /** 지역번호·휴대전화(02-123-4567, 010-1234-5678)와 대표번호(1588-1234)를 받는다. 하이픈은 없어도 된다. */
    @NotBlank(message = "회사 전화번호를 입력해주세요.")
    @Pattern(regexp = "^(0\\d{1,2}-?\\d{3,4}-?\\d{4}|1\\d{3}-?\\d{4})$",
            message = "회사 전화번호 형식이 올바르지 않습니다. 예) 02-123-4567, 1588-1234")
    private String companyPhone;

    private LocalDate interviewDate;
}
