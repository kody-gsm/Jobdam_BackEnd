package com.example.kodyjobdam.user.config;

import com.example.kodyjobdam.user.security.CustomUserDetailsService;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityConfigTest.ProbeController.class})
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockBean JwtTokenProvider tokens;
    @MockBean CustomUserDetailsService users;

    @Test
    void teacherApiReturns401ForExpiredToken403ForStudentAnd200ForTeacher() throws Exception {
        mvc.perform(get("/teacher/probe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/teacher/probe").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
        for (String role : new String[]{"STUDENT", "TEACHER"}) {
            var claims = Jwts.claims();
            claims.put("email", "test@example.com");
            when(tokens.validateToken(role)).thenReturn(true);
            when(tokens.getClaims(role)).thenReturn(claims);
            when(users.loadUserByUsername("test@example.com"))
                    .thenReturn(User.withUsername("test").password("unused").roles(role).build());
            mvc.perform(get("/teacher/probe").header("Authorization", "Bearer " + role))
                    .andExpect(role.equals("TEACHER") ? status().isOk() : status().isForbidden());
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/teacher/probe")
        String read() { return "ok"; }
    }
}
