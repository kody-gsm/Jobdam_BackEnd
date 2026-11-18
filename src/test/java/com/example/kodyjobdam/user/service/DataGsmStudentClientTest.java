package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.dto.DataGsmStudent;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DataGsmStudentClientTest {

    @Test
    void 전체_동기화는_졸업생과_자퇴생을_포함하고_null_학적정보를_읽는다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataGsmStudentClient client = new DataGsmStudentClient(builder);
        ReflectionTestUtils.setField(client, "baseUrl", "https://openapi.datagsm.kr");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        server.expect(request -> {
            assertThat(request.getURI().getPath()).isEqualTo("/v1/students");
            assertThat(request.getURI().getQuery())
                    .contains("includeGraduates=true", "includeWithdrawn=true")
                    .doesNotContain("onlyEnrolled=true");
        }).andRespond(withSuccess("""
                {"data":{"students":[{"id":1,"name":"김학생","email":"student@gsm.hs.kr",
                "grade":null,"classNum":null,"number":null,"studentNumber":null}]}}
                """, MediaType.APPLICATION_JSON));

        List<DataGsmStudent> students = client.fetchStudents(0, 300);

        assertThat(students).hasSize(1);
        assertThat(students.get(0).studentNumber()).isNull();
        assertThat(students.get(0).grade()).isNull();
        server.verify();
    }
}
