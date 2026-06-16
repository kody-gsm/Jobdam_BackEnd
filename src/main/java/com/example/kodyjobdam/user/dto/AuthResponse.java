package com.example.kodyjobdam.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse { //인증 결과 넘겨주기
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String name;
}