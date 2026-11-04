package com.example.kodyjobdam.passreport.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PassReportCreateDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 기업_이름만_있으면_보낼_수_있다() {
        assertThat(validator.validate(dto("카카오"))).isEmpty();
        assertThat(validator.validate(dto("가".repeat(100)))).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void 기업_이름이_비어_있으면_보낼_수_없다(String companyName) {
        assertThat(messages(dto(companyName))).containsExactly("기업 이름을 입력해주세요.");
    }

    @Test
    void 기업_이름이_100자를_넘으면_보낼_수_없다() {
        assertThat(messages(dto("가".repeat(101)))).containsExactly("기업 이름은 100자 이하로 입력해주세요.");
    }

    private List<String> messages(PassReportCreateDTO dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).toList();
    }

    private PassReportCreateDTO dto(String companyName) {
        PassReportCreateDTO dto = new PassReportCreateDTO();
        ReflectionTestUtils.setField(dto, "companyName", companyName);
        return dto;
    }
}
