package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.dto.DataGsmStudent;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    private DataGsmStudent student(String email) {
        return new DataGsmStudent(1L, "김학생", email, 2, 1, 5, 2105);
    }
}
