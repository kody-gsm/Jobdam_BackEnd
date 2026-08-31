package com.example.kodyjobdam.user.service;

import com.example.kodyjobdam.user.dto.DataGsmStudent;
import com.example.kodyjobdam.common.exception.ConfigException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataGsmStudentClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${datagsm.api.base-url:https://openapi.datagsm.kr}")
    private String baseUrl;

    @Value("${datagsm.api.key:}")
    private String apiKey;

    public List<DataGsmStudent> fetchStudents(int page, int size) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/students")
                .queryParam("onlyEnrolled", true)
                .queryParam("sortBy", "STUDENT_NUMBER")
                .queryParam("sortDirection", "ASC")
                .queryParam("page", page)
                .queryParam("size", size)
                .build()
                .toUri();

        DataGsmStudentApiResponse response = get(uri);
        if (response == null || response.data() == null || response.data().students() == null) {
            return List.of();
        }

        return response.data().students();
    }

    public DataGsmStudent fetchStudentByEmail(String email) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/students")
                .queryParam("email", email)
                .queryParam("onlyEnrolled", true)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .build()
                .toUri();

        DataGsmStudentApiResponse response = get(uri);
        if (response == null || response.data() == null || response.data().students() == null) {
            return null;
        }

        return response.data().students().stream().findFirst().orElse(null);
    }

    private DataGsmStudentApiResponse get(URI uri) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ConfigException.serviceUnavailable("DataGSM API key is not configured.");
        }

        return restClientBuilder.build()
                .get()
                .uri(uri)
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(DataGsmStudentApiResponse.class);
    }

    private record DataGsmStudentApiResponse(
            String status,
            int code,
            String message,
            Data data
    ) {
    }

    private record Data(
            int totalPages,
            long totalElements,
            List<DataGsmStudent> students
    ) {
    }
}
