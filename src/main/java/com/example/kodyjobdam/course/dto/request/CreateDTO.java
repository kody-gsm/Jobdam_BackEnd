package com.example.kodyjobdam.course.dto.request;

import com.example.kodyjobdam.common.entity.CounselingCategoryEnum;
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

    private CounselingCategoryEnum category;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String period;

    private Long teacherId;

    public CourseEntity toEntity(User teacher, String submitterHash, String encryptedTitle, String encryptedContent,
                                 String encryptedUserId, String encryptedUserName, String encryptedStudentNumber) {
        return CourseEntity.builder()
                .submitterHash(submitterHash)
                .encryptedTitle(encryptedTitle)
                .encryptedContent(encryptedContent)
                .category(category)
                .encryptedUserId(encryptedUserId)
                .encryptedUserName(encryptedUserName)
                .encryptedStudentNumber(encryptedStudentNumber)
                .date(date)
                .period(period)
                .state(StateEnum.WAITING)
                .teacher(teacher)
                .build();
    }
}
