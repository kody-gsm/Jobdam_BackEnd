package com.example.kodyjobdam.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();//SecurityContext = 로그인 정보 저장소|Authentication = 로그인 정보 객체
       // Authentication을 저장 → 로그인 완료 상태
        if (authentication == null || !authentication.isAuthenticated()) {//authentication.isAuthenticated() = "이 인증이 성공했냐?"
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal(); //이거 했을 때 null 나옴 → 로그인 안 된 상태,,현재 로그인한 사용자 정보 객체를 꺼낸다
        if (!(principal instanceof CustomUserDetails userDetails)) {//principal이 CustomUserDetails 맞냐? 아니면 에러,,
            throw new IllegalStateException("인증된 사용자 정보 형식이 올바르지 않습니다.");
        }

        return userDetails.getId();
    }
}
