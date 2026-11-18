package com.example.kodyjobdam.user.config;

import com.example.kodyjobdam.user.security.CustomUserDetailsService;
import com.example.kodyjobdam.user.security.CustomUserDetails;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, new BadCredentialsException("Invalid JWT."));
                return;
            }

            Claims claims = jwtTokenProvider.getClaims(token);
            String email = claims.get("email", String.class);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Object versionClaim = claims.get("authTokenVersion");
            long tokenVersion = versionClaim instanceof Number number ? number.longValue() : 0L;
            if (!(userDetails instanceof CustomUserDetails currentUser)
                    || !currentUser.isEnabled()
                    || !currentUser.getId().toString().equals(claims.getSubject())
                    || currentUser.getAuthTokenVersion() != tokenVersion) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, new BadCredentialsException("Inactive account or revoked JWT."));
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
