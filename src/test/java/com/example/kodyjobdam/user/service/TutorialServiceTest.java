package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TutorialServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TutorialService tutorialService = new TutorialService(userRepository);

    @Test
    void 이전_튜토리얼만_완료한_기존_계정은_새_튜토리얼을_다시_본다() {
        User user = User.builder().id(1L).tutorialCompleted(true).tutorialVersion(1).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(tutorialService.getStatus(1L).tutorialCompleted()).isFalse();
    }

    @Test
    void 버전_정보가_없는_계정은_튜토리얼을_본다() {
        User user = User.builder().id(1L).tutorialCompleted(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(tutorialService.getStatus(1L).tutorialCompleted()).isFalse();
    }

    @Test
    void 현재_튜토리얼을_완료하면_다시_보지_않는다() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(tutorialService.complete(1L).tutorialCompleted()).isTrue();
        assertThat(user.getTutorialVersion()).isEqualTo(TutorialService.CURRENT_TUTORIAL_VERSION);
        assertThat(tutorialService.getStatus(1L).tutorialCompleted()).isTrue();
    }
}
