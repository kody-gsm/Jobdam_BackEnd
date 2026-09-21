package com.example.kodyjobdam.user.config;

import com.example.kodyjobdam.user.security.CustomUserDetailsService;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** 보안 설정 빈만 띄워 본다. 전체 컨텍스트 테스트가 없어 빈 순환 참조가 배포 뒤에야 드러났다. */
@SpringJUnitWebConfig(SecurityConfigTest.Config.class)
class SecurityConfigTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class})
    static class Config {
    }

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void 보안_설정과_JWT_필터가_순환_참조_없이_만들어진다() {
        assertThat(securityFilterChain.getFilters()).hasAtLeastOneElementOfType(JwtAuthenticationFilter.class);
    }
}
