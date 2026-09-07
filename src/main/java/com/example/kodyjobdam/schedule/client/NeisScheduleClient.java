package com.example.kodyjobdam.schedule.client;

import com.example.kodyjobdam.common.exception.ConfigException;
import com.example.kodyjobdam.common.exception.ScheduleException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 나이스 교육정보 개방 포털의 학사일정(SchoolSchedule) API 클라이언트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeisScheduleClient {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RESULT_NO_DATA = "INFO-200";
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGE = 10;

    private static final String BASE_URL = "https://open.neis.go.kr";
    /** 시도교육청코드 - 광주 */
    private static final String OFFICE_CODE = "F10";
    /** 표준학교코드 - 광주소프트웨어마이스터고등학교 */
    private static final String SCHOOL_CODE = "7140392";

    private final RestClient.Builder restClientBuilder;

    @Value("${neis.api.key:}")
    private String apiKey;

    public List<NeisScheduleRow> fetchSchedules(LocalDate from, LocalDate to) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ConfigException.serviceUnavailable("NEIS API key is not configured.");
        }

        List<NeisScheduleRow> rows = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGE; page++) {
            List<NeisScheduleRow> fetched = fetchPage(from, to, page);
            rows.addAll(fetched);

            if (fetched.size() < PAGE_SIZE) {
                break;
            }
        }
        return rows;
    }

    private List<NeisScheduleRow> fetchPage(LocalDate from, LocalDate to, int page) {
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/hub/SchoolSchedule")
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", page)
                .queryParam("pSize", PAGE_SIZE)
                .queryParam("ATPT_OFCDC_SC_CODE", OFFICE_CODE)
                .queryParam("SD_SCHUL_CODE", SCHOOL_CODE)
                .queryParam("AA_FROM_YMD", from.format(YMD))
                .queryParam("AA_TO_YMD", to.format(YMD))
                .build()
                .toUri();

        NeisScheduleApiResponse response = restClientBuilder.build()
                .get()
                .uri(uri)
                .retrieve()
                .body(NeisScheduleApiResponse.class);

        if (response == null) {
            throw ScheduleException.badGateway("나이스 학사일정 응답이 비어 있습니다.");
        }

        // 조회 실패·결과 없음은 래퍼 없이 최상위 RESULT 로만 내려온다.
        if (response.schoolSchedule() == null) {
            NeisResult result = response.result();
            if (result != null && RESULT_NO_DATA.equals(result.code())) {
                return List.of();
            }

            String code = result == null ? "UNKNOWN" : result.code();
            String message = result == null ? "알 수 없는 오류" : result.message();
            log.warn("나이스 학사일정 조회 실패. code={}, message={}", code, message);
            throw ScheduleException.badGateway("나이스 학사일정을 불러오지 못했습니다. (" + code + ")");
        }

        return response.schoolSchedule().stream()
                .map(NeisScheduleBlock::row)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisScheduleApiResponse(
            @JsonProperty("SchoolSchedule") List<NeisScheduleBlock> schoolSchedule,
            @JsonProperty("RESULT") NeisResult result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisScheduleBlock(
            @JsonProperty("row") List<NeisScheduleRow> row
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisResult(
            @JsonProperty("CODE") String code,
            @JsonProperty("MESSAGE") String message
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NeisScheduleRow(
            @JsonProperty("AA_YMD") String date,
            @JsonProperty("EVENT_NM") String eventName,
            @JsonProperty("EVENT_CNTNT") String eventContent,
            @JsonProperty("SBTR_DD_SC_NM") String holidayType,
            @JsonProperty("ONE_GRADE_EVENT_YN") String firstGrade,
            @JsonProperty("TW_GRADE_EVENT_YN") String secondGrade,
            @JsonProperty("THREE_GRADE_EVENT_YN") String thirdGrade
    ) {
    }
}
