package com.example.kodyjobdam.schedule.service;

import com.example.kodyjobdam.common.exception.ScheduleException;
import com.example.kodyjobdam.schedule.client.NeisScheduleClient;
import com.example.kodyjobdam.schedule.client.NeisScheduleClient.NeisScheduleRow;
import com.example.kodyjobdam.schedule.dto.response.ScheduleReadDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private NeisScheduleClient neisScheduleClient;

    @InjectMocks
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduleService, "cacheTtlMinutes", 60L);
    }

    @Test
    void 나이스_응답을_날짜순으로_변환한다() {
        when(neisScheduleClient.fetchSchedules(any(), any())).thenReturn(List.of(
                row("20260302", "입학식", "", "", "Y", "*", "*"),
                row("20260301", "3·1절", "", "공휴일", "Y", "Y", "Y")
        ));

        List<ScheduleReadDTO> result = scheduleService.readSchedules(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(result.get(0).getName()).isEqualTo("3·1절");
        assertThat(result.get(0).getHolidayType()).isEqualTo("공휴일");
        assertThat(result.get(0).isHoliday()).isTrue();
        assertThat(result.get(0).getGrades()).containsExactly(1, 2, 3);

        assertThat(result.get(1).getName()).isEqualTo("입학식");
        assertThat(result.get(1).getContent()).isNull();
        assertThat(result.get(1).isHoliday()).isFalse();
        assertThat(result.get(1).getGrades()).containsExactly(1);
    }

    @Test
    void 학년으로_일정을_거른다() {
        when(neisScheduleClient.fetchSchedules(any(), any())).thenReturn(List.of(
                row("20260302", "입학식", "", "", "Y", "*", "*"),
                row("20260303", "3학년 현장실습", "", "", "*", "*", "Y")
        ));

        List<ScheduleReadDTO> result = scheduleService.readSchedules(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("3학년 현장실습");
    }

    @Test
    void 같은_구간을_다시_조회하면_캐시를_사용한다() {
        when(neisScheduleClient.fetchSchedules(any(), any())).thenReturn(List.of(
                row("20260301", "3·1절", "", "공휴일", "Y", "Y", "Y")
        ));

        scheduleService.readSchedules(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);
        scheduleService.readSchedules(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        verify(neisScheduleClient, times(1)).fetchSchedules(any(), any());
    }

    @Test
    void 날짜_형식이_깨진_일정은_제외한다() {
        when(neisScheduleClient.fetchSchedules(any(), any())).thenReturn(List.of(
                row("깨진값", "알 수 없음", "", "", "Y", "Y", "Y"),
                row("20260301", "3·1절", "", "공휴일", "Y", "Y", "Y")
        ));

        List<ScheduleReadDTO> result = scheduleService.readSchedules(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("3·1절");
    }

    @Test
    void 시작일이_종료일보다_늦으면_예외가_발생한다() {
        assertThatThrownBy(() -> scheduleService.readSchedules(
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 1), null))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("종료일");

        verify(neisScheduleClient, never()).fetchSchedules(any(), any());
    }

    @Test
    void 조회_기간이_1년을_넘으면_예외가_발생한다() {
        assertThatThrownBy(() -> scheduleService.readSchedules(
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 1), null))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("최대 1년");

        verify(neisScheduleClient, never()).fetchSchedules(any(), any());
    }

    @Test
    void 학년_값이_범위를_벗어나면_예외가_발생한다() {
        assertThatThrownBy(() -> scheduleService.readSchedules(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 4))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("학년");

        verify(neisScheduleClient, never()).fetchSchedules(any(), any());
    }

    @Test
    void 월별_조회는_해당_월의_시작일과_말일로_조회한다() {
        when(neisScheduleClient.fetchSchedules(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(row("20260201", "방학", "", "", "Y", "Y", "Y")));

        List<ScheduleReadDTO> result = scheduleService.readMonthlySchedules(2026, 2, null);

        assertThat(result).hasSize(1);
        verify(neisScheduleClient).fetchSchedules(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void 월이_범위를_벗어나면_예외가_발생한다() {
        assertThatThrownBy(() -> scheduleService.readMonthlySchedules(2026, 13, null))
                .isInstanceOf(ScheduleException.class)
                .hasMessageContaining("월은");

        verify(neisScheduleClient, never()).fetchSchedules(any(), any());
    }

    private NeisScheduleRow row(String date, String name, String content, String holidayType,
                                String first, String second, String third) {
        return new NeisScheduleRow(date, name, content, holidayType, first, second, third);
    }
}
