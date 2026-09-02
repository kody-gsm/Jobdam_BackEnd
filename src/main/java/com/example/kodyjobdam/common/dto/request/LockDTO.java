package com.example.kodyjobdam.common.dto.request;


import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
import com.example.kodyjobdam.user.entity.User;

import java.time.LocalDate;

public class LockDTO {

    private LocalDate date;

    private String period;

    private StateEnum state;

    public LocalDate getDate() {
        return date;
    }

    public String getPeriod() {
        return period;
    }

    /*public CommonEntity toEntity2(LockDTO dto) {
        CommonEntity entity = new CommonEntity();
        entity.setPeriod(dto.period);
        entity.setDate(dto.date);
        entity.setState(StateEnum.LOCKED);
        return entity;
    }*/

    //toEntity를 만들고 싶어 위 toEntity2의 값은 그냥 저대로 하고 새로운 toEntity를 만들어줘, CommonEntity에 있는 필드값들에 기본값을 넣어주었으면 좋겠어
    public CommonEntity toEntity(LockDTO dto, User teacher) {
        return CommonEntity.builder()
                .date(dto.date)
                .period(dto.period)
                .state(StateEnum.LOCKED)
                .title("Locked")
                .content("이 시간은 잠긴 시간입니다.")
                .user(null)
                .teacher(teacher)
                .build();
    }

}
