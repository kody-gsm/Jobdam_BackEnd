package com.example.kodyjobdam.passreport.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 학생이 보내는 면접 합격 보고.
 *
 * <p>보내는 학생과 그 학번·이름은 토큰에서 정하므로 기업 이름만 받는다.</p>
 */
@Getter
public class PassReportCreateDTO {

    @NotBlank(message = "기업 이름을 입력해주세요.")
    @Size(max = 100, message = "기업 이름은 100자 이하로 입력해주세요.")
    private String companyName;
}
