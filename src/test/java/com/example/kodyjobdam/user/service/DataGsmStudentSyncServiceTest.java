package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.DataGsmStudent;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataGsmStudentSyncServiceTest {

    @Mock
    private DataGsmStudentClient dataGsmStudentClient;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataGsmStudentSyncService dataGsmStudentSyncService;

    @Test
    void 요청한_메일과_다른_학생이_오면_가져오지_않는다() {
        when(dataGsmStudentClient.fetchStudentByEmail("25035@gsm.hs.kr"))
                .thenReturn(student("s25035@gsm.hs.kr"));

        assertThat(dataGsmStudentSyncService.syncStudentByEmail("25035@gsm.hs.kr")).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void 요청한_메일의_학생이면_대소문자가_달라도_가져온다() {
        when(dataGsmStudentClient.fetchStudentByEmail("S25035@gsm.hs.kr"))
                .thenReturn(student("s25035@gsm.hs.kr"));
        when(userRepository.findByDataGsmStudentId(1L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("s25035@gsm.hs.kr")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        User user = dataGsmStudentSyncService.syncStudentByEmail("S25035@gsm.hs.kr");

        assertThat(user.getEmail()).isEqualTo("s25035@gsm.hs.kr");
        assertThat(user.getRole()).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void 졸업생의_학적_정보가_null이면_기존_계정에도_null로_반영한다() {
        User existing = User.builder()
                .id(7L)
                .name("김학생")
                .email("s25035@gsm.hs.kr")
                .student_number("2105")
                .grade(2)
                .classNum(1)
                .number(5)
                .role(UserRole.STUDENT)
                .build();
        DataGsmStudent graduate = new DataGsmStudent(
                1L, "김학생", "s25035@gsm.hs.kr", null, null, null, null);
        when(dataGsmStudentClient.fetchStudents(0, 300)).thenReturn(List.of(graduate));
        when(userRepository.findByDataGsmStudentId(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        assertThat(dataGsmStudentSyncService.syncAllStudents()).isEqualTo(1);

        assertThat(existing.getStudent_number()).isNull();
        assertThat(existing.getGrade()).isNull();
        assertThat(existing.getClassNum()).isNull();
        assertThat(existing.getNumber()).isNull();
        assertThat(existing.getRole()).isEqualTo(UserRole.STUDENT);
        verify(userRepository).save(existing);
    }

    @Test
    void 학번이_null인_학생도_새_계정으로_저장한다() {
        DataGsmStudent graduate = new DataGsmStudent(
                2L, "이졸업", "graduate@gsm.hs.kr", null, null, null, null);
        when(dataGsmStudentClient.fetchStudents(0, 300)).thenReturn(List.of(graduate));
        when(userRepository.findByDataGsmStudentId(2L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("graduate@gsm.hs.kr")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        assertThat(dataGsmStudentSyncService.syncAllStudents()).isEqualTo(1);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getStudent_number()).isNull();
        assertThat(saved.getValue().getClassNum()).isNull();
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.STUDENT);
    }

    private DataGsmStudent student(String email) {
        return new DataGsmStudent(1L, "김학생", email, 2, 1, 5, 2105);
    }
}
