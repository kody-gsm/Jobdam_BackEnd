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

    private final UserRepository userRepository;

    public TutorialStatusResponse getStatus(Long userId) {
        return new TutorialStatusResponse(findUser(userId).isTutorialCompleted());
    }

    @Transactional
    public TutorialStatusResponse complete(Long userId) {
        User user = findUser(userId);
        user.completeTutorial();
        return new TutorialStatusResponse(user.isTutorialCompleted());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }
}
