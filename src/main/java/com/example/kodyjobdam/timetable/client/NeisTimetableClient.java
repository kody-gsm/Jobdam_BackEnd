package com.example.kodyjobdam.timetable.client;

import com.example.kodyjobdam.common.exception.ConfigException;
import com.example.kodyjobdam.common.exception.ScheduleException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 나이스 교육정보 개방 포털의 고등학교 시간표(hisTimetable) API 클라이언트.
 *
 * <p>시간표는 학급 단위라 학년·반으로만 조회할 수 있다. 교사별 시간표는 제공되지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeisTimetableClient {

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

    /** 학년도·학기는 조회 기간으로 정해지므로 따로 넘기지 않는다. */
    public List<NeisTimetableRow> fetchTimetables(int grade, int classNum, LocalDate from, LocalDate to) {
        if (apiKey == null || apiKey.isBlank()) {
            throw ConfigException.serviceUnavailable("NEIS API key is not configured.");
        }

        List<NeisTimetableRow> rows = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGE; page++) {
            List<NeisTimetableRow> fetched = fetchPage(grade, classNum, from, to, page);
            rows.addAll(fetched);

            if (fetched.size() < PAGE_SIZE) {
                break;
            }
        }
        return rows;
    }

    private List<NeisTimetableRow> fetchPage(int grade, int classNum, LocalDate from, LocalDate to, int page) {
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/hub/hisTimetable")
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", page)
                .queryParam("pSize", PAGE_SIZE)
                .queryParam("ATPT_OFCDC_SC_CODE", OFFICE_CODE)
                .queryParam("SD_SCHUL_CODE", SCHOOL_CODE)
                .queryParam("GRADE", grade)
                .queryParam("CLASS_NM", classNum)
                .queryParam("TI_FROM_YMD", from.format(YMD))
                .queryParam("TI_TO_YMD", to.format(YMD))
                .build()
                .toUri();

        NeisTimetableApiResponse response;
        try {
            response = restClientBuilder.build()
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body(NeisTimetableApiResponse.class);
        } catch (RestClientException e) {
            // 타임아웃·연결 실패도 나이스 장애로 본다. 그대로 두면 처리되지 않은 예외로 500이 된다.
            log.warn("나이스 시간표 요청 실패. grade={}, classNum={}, from={}, to={}, page={}",
                    grade, classNum, from, to, page, e);
            throw ScheduleException.badGateway("나이스 시간표를 불러오지 못했습니다.");
        }

        if (response == null) {
            throw ScheduleException.badGateway("나이스 시간표 응답이 비어 있습니다.");
        }

        // 조회 실패·결과 없음은 래퍼 없이 최상위 RESULT 로만 내려온다.
        if (response.timetable() == null) {
            NeisResult result = response.result();
            if (result != null && RESULT_NO_DATA.equals(result.code())) {
                return List.of();
            }

            String code = result == null ? "UNKNOWN" : result.code();
            String message = result == null ? "알 수 없는 오류" : result.message();
            log.warn("나이스 시간표 조회 실패. code={}, message={}", code, message);
            throw ScheduleException.badGateway("나이스 시간표를 불러오지 못했습니다. (" + code + ")");
        }

        return response.timetable().stream()
                .map(NeisTimetableBlock::row)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisTimetableApiResponse(
            @JsonProperty("hisTimetable") List<NeisTimetableBlock> timetable,
            @JsonProperty("RESULT") NeisResult result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisTimetableBlock(
            @JsonProperty("row") List<NeisTimetableRow> row
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NeisResult(
            @JsonProperty("CODE") String code,
            @JsonProperty("MESSAGE") String message
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NeisTimetableRow(
            @JsonProperty("ALL_TI_YMD") String date,
            @JsonProperty("PERIO") String period,
            @JsonProperty("ITRT_CNTNT") String subject,
            @JsonProperty("CLRM_NM") String classroom
    ) {
    }
}
