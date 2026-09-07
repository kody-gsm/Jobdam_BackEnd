package com.example.kodyjobdam.notification.controller;

import com.example.kodyjobdam.notification.service.NotificationService;
import com.example.kodyjobdam.notification.service.NotificationSseService;
import com.example.kodyjobdam.notification.service.SseTicketService;
import com.example.kodyjobdam.user.security.SecurityUtil;
import com.example.kodyjobdam.user.security.CustomUserDetailsService;
import com.example.kodyjobdam.user.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SseTicketService.class)
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SseTicketService tickets;
    @MockBean NotificationService notifications;
    @MockBean NotificationSseService streams;
    @MockBean SecurityUtil security;
    @MockBean JwtTokenProvider tokens;
    @MockBean CustomUserDetailsService users;

    @Test
    void ticketEndpointIssuesSerializableSingleUseTicket() throws Exception {
        when(security.getCurrentUserId()).thenReturn(7L);
        var result = mvc.perform(post("/api/notifications/subscribe-ticket"))
                .andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        String ticket = body.get("ticket").asText();
        assertThat(ticket).isNotBlank();
        assertThat(body.get("expiresAt").asText()).isNotBlank();
        assertThat(tickets.consume(ticket)).isEqualTo(7L);
        assertThatThrownBy(() -> tickets.consume(ticket))
                .isInstanceOf(com.example.kodyjobdam.common.exception.NotificationException.class);
    }
}
