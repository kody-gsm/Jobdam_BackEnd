package com.example.kodyjobdam.passreport.dto.request;

import com.example.kodyjobdam.passreport.entity.PassType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PassReportCreateDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"02-123-4567", "062-123-4567", "010-1234-5678", "0212345678", "1588-1234", "15881234"})
    void 회사_전화번호로_받는_형식(String phone) {
        assertThat(validator.validate(dto(phone))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123-4567", "02-12-34", "010-1234-56789", "전화번호", "02 123 4567", "+82-2-123-4567"})
    void 회사_전화번호로_받지_않는_형식(String phone) {
        Set<ConstraintViolation<PassReportCreateDTO>> violations = validator.validate(dto(phone));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("companyPhone");
    }

    private PassReportCreateDTO dto(String phone) {
        PassReportCreateDTO dto = new PassReportCreateDTO();
        ReflectionTestUtils.setField(dto, "teacherId", 2L);
        ReflectionTestUtils.setField(dto, "type", PassType.INTERVIEW);
        ReflectionTestUtils.setField(dto, "companyName", "카카오");
        ReflectionTestUtils.setField(dto, "companyPhone", phone);
        return dto;
    }
}
