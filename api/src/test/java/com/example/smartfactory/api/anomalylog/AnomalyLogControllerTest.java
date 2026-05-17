package com.example.smartfactory.api.anomalylog;

import com.example.smartfactory.api.anomalylog.dto.AnomalyLogResponse;
import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.common.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnomalyLogControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @Mock
    private AnomalyLogService anomalyLogService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AnomalyLogController(anomalyLogService))
                .setConversionService(new DefaultFormattingConversionService())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    private PagedResponse<AnomalyLogResponse> sampleResponse(int page, int perPage) {
        AnomalyLogResponse item = new AnomalyLogResponse(
                1L, "room01", "temperature",
                new BigDecimal("35.0"), new BigDecimal("38.2"),
                "温度が上限閾値(35.0℃)を超えました: 38.2℃",
                OffsetDateTime.parse("2026-01-15T10:00:00Z"));
        return new PagedResponse<>(List.of(item), new PagedResponse.Pagination(1, page, perPage));
    }

    @Test
    @DisplayName("GET /api/anomaly-logs はパラメータなしで 200 とデータを返すこと")
    void getAnomalyLogs_noParams_returns200() throws Exception {
        given(anomalyLogService.findAll(
                eq(UUID.fromString(USER_ID)), isNull(), isNull(), isNull(), isNull(), eq(1), eq(20)))
                .willReturn(sampleResponse(1, 20));

        mvc.perform(get("/api/anomaly-logs").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].device_id").value("room01"))
                .andExpect(jsonPath("$.data[0].metric_type").value("temperature"))
                .andExpect(jsonPath("$.data[0].threshold_value").value(35.0))
                .andExpect(jsonPath("$.data[0].actual_value").value(38.2))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.per_page").value(20));
    }

    @Test
    @DisplayName("device_id と metric_type フィルタが指定された場合はサービスに渡されること")
    void getAnomalyLogs_withFilters_passesToService() throws Exception {
        given(anomalyLogService.findAll(
                eq(UUID.fromString(USER_ID)), eq("room01"), eq("temperature"),
                isNull(), isNull(), eq(1), eq(20)))
                .willReturn(sampleResponse(1, 20));

        mvc.perform(get("/api/anomaly-logs")
                        .session(session())
                        .param("device_id", "room01")
                        .param("metric_type", "temperature"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("from/to パラメータが指定された場合はサービスに渡されること")
    void getAnomalyLogs_withDateRange_passesToService() throws Exception {
        given(anomalyLogService.findAll(
                eq(UUID.fromString(USER_ID)), isNull(), isNull(), any(), any(), eq(1), eq(20)))
                .willReturn(sampleResponse(1, 20));

        mvc.perform(get("/api/anomaly-logs")
                        .session(session())
                        .param("from", "2026-01-15T00:00:00Z")
                        .param("to", "2026-01-15T23:59:59Z"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("page と per_page パラメータが指定された場合はサービスに渡されること")
    void getAnomalyLogs_withPagination_passesToService() throws Exception {
        given(anomalyLogService.findAll(
                eq(UUID.fromString(USER_ID)), isNull(), isNull(), isNull(), isNull(), eq(2), eq(50)))
                .willReturn(sampleResponse(2, 50));

        mvc.perform(get("/api/anomaly-logs")
                        .session(session())
                        .param("page", "2")
                        .param("per_page", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(2))
                .andExpect(jsonPath("$.pagination.per_page").value(50));
    }

    @Test
    @DisplayName("page が 0 の場合 400 を返すこと")
    void getAnomalyLogs_pageZero_returns400() throws Exception {
        given(anomalyLogService.findAll(any(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .willThrow(new IllegalArgumentException("page は1以上を指定してください"));

        mvc.perform(get("/api/anomaly-logs")
                        .session(session())
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("metric_type が不正値の場合 400 を返すこと")
    void getAnomalyLogs_invalidMetricType_returns400() throws Exception {
        given(anomalyLogService.findAll(
                any(), isNull(), eq("invalid"), isNull(), isNull(), eq(1), eq(20)))
                .willThrow(new IllegalArgumentException("metric_type の値が不正です: invalid"));

        mvc.perform(get("/api/anomaly-logs")
                        .session(session())
                        .param("metric_type", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
