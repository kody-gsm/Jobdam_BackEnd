package com.example.kodyjobdam.common.dto.response;

import com.example.kodyjobdam.common.entity.StateEnum;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class SlotStatusDTO {

    private final Long teacherId;

    private final LocalDate date;

    private final String period;

    private final StateEnum state;

    private final boolean available;

    public SlotStatusDTO(Long teacherId, LocalDate date, String period, StateEnum state) {
        this.teacherId = teacherId;
        this.date = date;
        this.period = period;
        this.state = state;
        this.available = state != StateEnum.LOCKED && state != StateEnum.RESERVED;
    }
}
