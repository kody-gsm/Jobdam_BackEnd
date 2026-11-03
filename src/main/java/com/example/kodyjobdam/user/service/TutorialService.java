package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.UserRepository;
import com.example.kodyjobdam.user.dto.TutorialStatusResponse;
import com.example.kodyjobdam.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorialService {

    // 튜토리얼 내용이 크게 바뀌면 올린다. 1: 예시 화면 튜토리얼, 2: 실제 화면 튜토리얼
    public static final int CURRENT_TUTORIAL_VERSION = 2;

    private final UserRepository userRepository;

    public TutorialStatusResponse getStatus(Long userId) {
        return new TutorialStatusResponse(findUser(userId).hasCompletedTutorial(CURRENT_TUTORIAL_VERSION));
    }

    @Transactional
    public TutorialStatusResponse complete(Long userId) {
        User user = findUser(userId);
        user.completeTutorial(CURRENT_TUTORIAL_VERSION);
        return new TutorialStatusResponse(user.hasCompletedTutorial(CURRENT_TUTORIAL_VERSION));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
