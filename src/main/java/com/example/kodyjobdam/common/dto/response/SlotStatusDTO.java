package com.example.kodyjobdam.common.dto.response;

import com.example.kodyjobdam.common.entity.StateEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class SlotStatusDTO {

    private final Long teacherId;

    private final LocalDate date;

    private final String period;

    /** 이 선생님 시간의 상태. 남의 예약도 잠금도 없으면 null 이다. */
    private final StateEnum state;

    /** 보는 학생이 이 시간에 이미 신청해 둔 상담이 있다. 선생님이 조회하면 항상 false 다. */
    private final boolean mine;

    private final boolean available;

    public SlotStatusDTO(Long teacherId, LocalDate date, String period, StateEnum state, boolean mine) {
        this.teacherId = teacherId;
        this.date = date;
        this.period = period;
        this.state = state;
        this.mine = mine;
        // 이미 신청한 시간에는 선생님을 바꿔도 다시 신청할 수 없다.
        this.available = !mine && state != StateEnum.LOCKED && state != StateEnum.RESERVED;
    }
}
