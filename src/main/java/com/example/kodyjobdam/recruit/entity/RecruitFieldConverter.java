package com.example.kodyjobdam.recruit.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 직무 분야 목록을 쉼표로 이어 한 컬럼에 담는다.
 *
 * <p>공고당 분야는 많아야 몇 개이고 분야별 필터는 메모리에서 하므로 자식 테이블을 두지 않는다.
 * 새 분야를 추가할 때 DDL을 건드릴 필요가 없다는 이점도 있다.</p>
 *
 * <p>저장할 때는 enum 선언 순서로 정렬해 같은 조합이 언제나 같은 문자열이 되게 한다.
 * 읽을 때는 모르는 값을 버린다. enum에서 값을 지우거나 이름을 바꿔도 옛 행 때문에
 * 조회 전체가 깨지지 않게 하기 위한 처리다.</p>
 */
@Slf4j
@Converter
public class RecruitFieldConverter implements AttributeConverter<Set<RecruitField>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<RecruitField> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        return fields.stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<RecruitField> convertToEntityAttribute(String column) {
        Set<RecruitField> fields = EnumSet.noneOf(RecruitField.class);
        if (column == null || column.isBlank()) {
            return fields;
        }

        for (String token : column.split(DELIMITER)) {
            String name = token.trim();
            if (name.isEmpty()) {
                continue;
            }

            try {
                fields.add(RecruitField.valueOf(name));
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 직무 분야가 저장되어 있습니다: {}", name);
            }
        }
        return fields;
    }
}
