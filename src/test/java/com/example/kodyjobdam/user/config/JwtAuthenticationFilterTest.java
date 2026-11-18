package com.example.kodyjobdam.user.config;

import com.example.kodyjobdam.user.UserRole;
import com.example.kodyjobdam.user.entity.User;
import com.example.kodyjobdam.user.security.CustomUserDetails;
import com.example.kodyjobdam.user.security.CustomUserDetailsService;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void 탈퇴한_계정은_기존_접근_토큰으로_인증되지_않는다() throws Exception {
        assertRejected(user(null, false, 1), 0);
    }

    @Test
    void 재가입해도_탈퇴_전_접근_토큰은_인증되지_않는다() throws Exception {
        assertRejected(user("new-password-hash", true, 1), 0);
    }

    @Test
    void 탈퇴_이력이_없는_계정은_기존_형식의_토큰도_인증된다() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        Claims claims = mock(Claims.class);
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> response.setStatus(401);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.addHeader("Authorization", "Bearer old-format-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenProvider.validateToken("old-format-token")).thenReturn(true);
        when(tokenProvider.getClaims("old-format-token")).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("student@gsm.hs.kr");
        when(claims.getSubject()).thenReturn("10");
        when(userDetailsService.loadUserByUsername("student@gsm.hs.kr"))
                .thenReturn(new CustomUserDetails(user("password-hash", true, 0)));

        try {
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertRejected(User user, int tokenVersion) throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        Claims claims = mock(Claims.class);
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> response.setStatus(401);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.addHeader("Authorization", "Bearer old-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenProvider.validateToken("old-token")).thenReturn(true);
        when(tokenProvider.getClaims("old-token")).thenReturn(claims);
        when(claims.get("email", String.class)).thenReturn("student@gsm.hs.kr");
        when(claims.getSubject()).thenReturn("10");
        when(claims.get("authTokenVersion")).thenReturn(tokenVersion);
        when(userDetailsService.loadUserByUsername("student@gsm.hs.kr"))
                .thenReturn(new CustomUserDetails(user));

        try {
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(chain.getRequest()).isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private User user(String password, boolean verified, long tokenVersion) {
        return User.builder().id(10L).name("김학생").email("student@gsm.hs.kr")
                .password(password).emailVerified(verified).role(UserRole.STUDENT)
                .authTokenVersion(tokenVersion).build();
    }
}
