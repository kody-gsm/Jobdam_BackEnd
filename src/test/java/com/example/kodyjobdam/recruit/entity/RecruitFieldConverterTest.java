package com.example.kodyjobdam.recruit.entity;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitFieldConverterTest {

    private final RecruitFieldConverter converter = new RecruitFieldConverter();

    @Test
    void 분야를_쉼표로_이어_저장한다() {
        String column = converter.convertToDatabaseColumn(
                EnumSet.of(RecruitField.BACKEND, RecruitField.FRONTEND));

        // enum 선언 순서로 정렬해 같은 조합이 언제나 같은 문자열이 되게 한다.
        assertThat(column).isEqualTo("FRONTEND,BACKEND");
    }

    @Test
    void 분야가_없으면_null로_저장한다() {
        assertThat(converter.convertToDatabaseColumn(Set.of())).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void 저장된_문자열을_분야로_되돌린다() {
        assertThat(converter.convertToEntityAttribute("FRONTEND,BACKEND"))
                .containsExactly(RecruitField.FRONTEND, RecruitField.BACKEND);
    }

    @Test
    void 빈_컬럼은_빈_분야로_읽는다() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
        assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
    }

    @Test
    void 모르는_값은_버리고_나머지를_살린다() {
        // enum에서 값을 지우거나 이름을 바꿔도 옛 행 때문에 조회가 깨지지 않아야 한다.
        assertThat(converter.convertToEntityAttribute("BACKEND,DEVOPS,,AI"))
                .containsExactly(RecruitField.BACKEND, RecruitField.AI);
    }

    @Test
    void 저장과_읽기를_왕복해도_같은_값이다() {
        Set<RecruitField> fields = EnumSet.allOf(RecruitField.class);

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(fields)))
                .isEqualTo(fields);
    }

    @Test
    void 모든_분야를_담아도_컬럼_길이를_넘지_않는다() {
        String column = converter.convertToDatabaseColumn(EnumSet.allOf(RecruitField.class));

        // RecruitEntity.fields의 @Column(length = 100)
        assertThat(column).hasSizeLessThanOrEqualTo(100);
    }
}
