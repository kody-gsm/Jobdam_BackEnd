package com.example.kodyjobdam.common.dto.request;

import com.example.kodyjobdam.common.entity.CommonEntity;
import com.example.kodyjobdam.common.entity.StateEnum;
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

    private Long teacherId;

    public CommonEntity toEntity(User teacher, String submitterHash, String encryptedTitle, String encryptedContent,
                                 String encryptedUserId, String encryptedUserName, String encryptedStudentNumber) {
        return CommonEntity.builder()
                .submitterHash(submitterHash)
                .encryptedTitle(encryptedTitle)
                .encryptedContent(encryptedContent)
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
