package com.example.kodyjobdam.timetable.dto.response;

import lombok.Getter;

import java.time.LocalDate;

/**
 * 학급 시간표 한 칸. 상담 신청 화면에 참고로만 보여준다.
 *
 * <p>나이스 시간표에는 점심시간·저녁시간이 없으므로 그 교시는 응답에 담기지 않는다.</p>
 */
@Getter
public class TimetableReadDTO {

    private final LocalDate date;

    /** 상담 예약 API와 같은 교시 라벨("1교시"). 시간표에 없는 교시는 담지 않는다. */
    private final String period;

    /** 과목명 */
    private final String subject;

    /** 강의실. 나이스가 비워 두면 null 이다. */
    private final String classroom;

    public TimetableReadDTO(LocalDate date, String period, String subject, String classroom) {
        this.date = date;
        this.period = period;
        this.subject = subject;
        this.classroom = classroom;
    }
}
