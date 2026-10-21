package com.example.kodyjobdam.timetable.service;

import com.example.kodyjobdam.timetable.client.NeisTimetableClient;
import com.example.kodyjobdam.timetable.client.NeisTimetableClient.NeisTimetableRow;
import com.example.kodyjobdam.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private NeisTimetableClient neisTimetableClient;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TimetableService timetableService;

    @Test
    void 학교_전체_시간표에서_과목이_든_날짜별_교시를_모은다() {
        LocalDate from = LocalDate.of(2026, 10, 12);
        LocalDate to = LocalDate.of(2026, 10, 16);
        when(neisTimetableClient.fetchSchoolTimetables(from, to)).thenReturn(List.of(
                new NeisTimetableRow("20261013", "2", "진로활동", "1"),
                new NeisTimetableRow("20261013", "2", "진로활동", "3"),     // 다른 반, 같은 교시
                new NeisTimetableRow("20261013", "1", "진로활동", "1"),
                new NeisTimetableRow("20261013", "3", "자바 프로그래밍", "1"),
                new NeisTimetableRow("20261015", "4", "*진로활동", "1"),
                new NeisTimetableRow("20261015", "10", "진로활동", "1")     // 상담 시간표에 없는 교시
        ));

        Map<LocalDate, Set<String>> periods = timetableService.findSubjectPeriods(from, to, Set.of("진로활동"));

        assertThat(periods).containsOnlyKeys(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 15));
        assertThat(periods.get(LocalDate.of(2026, 10, 13))).containsExactly("1교시", "2교시");
        assertThat(periods.get(LocalDate.of(2026, 10, 15))).containsExactly("4교시");
    }
}
