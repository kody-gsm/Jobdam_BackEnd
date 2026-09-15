package com.example.kodyjobdam.schedule.client;

import com.example.kodyjobdam.common.exception.ScheduleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

class NeisScheduleClientTest {

    @Test
    void 나이스_요청이_타임아웃나면_502_예외로_바꾼다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("Read timed out")));

        NeisScheduleClient client = new NeisScheduleClient(builder);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        assertThatThrownBy(() -> client.fetchSchedules(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .isInstanceOf(ScheduleException.class)
                .hasMessage("나이스 학사일정을 불러오지 못했습니다.");
    }
}
