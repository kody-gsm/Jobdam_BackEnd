package com.example.kodyjobdam.course.dto.request;

import com.example.kodyjobdam.course.entity.CourseEntity;
import com.example.kodyjobdam.course.entity.StateEnum;
import com.example.kodyjobdam.user.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class CreateDTO {

    private String title;

    private String content;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String period;

    public CourseEntity toEntity(User user) {
        return CourseEntity.builder()
                .title(title)
                .content(content)
                .date(date)
                .period(period)
                .state(StateEnum.WAITING)
                .user(user)
                .build();
    }
}
