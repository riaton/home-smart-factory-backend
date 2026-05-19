package com.example.smartfactory.api.threshold;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdResponse;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnomalyThresholdControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    private static final String THRESHOLD_ID = "00000000-0000-0000-0000-000000000002";

    @Mock
    private AnomalyThresholdService thresholdService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AnomalyThresholdController(thresholdService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    private AnomalyThresholdResponse sampleResponse() {
        return new AnomalyThresholdResponse(
                UUID.fromString(THRESHOLD_ID),
                "temperature",
                new BigDecimal("10.0"),
                new BigDecimal("35.0"),
                true,
                LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));
    }

    @Test
    @DisplayName("GET /api/anomaly-thresholds は 200 と閾値設定一覧を返すこと")
    void getAll_returns200WithList() throws Exception {
        given(thresholdService.getAll(UUID.fromString(USER_ID))).willReturn(List.of(sampleResponse()));

        mvc.perform(get("/api/anomaly-thresholds").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].metric_type").value("temperature"))
                .andExpect(jsonPath("$.data[0].min_value").value(10.0))
                .andExpect(jsonPath("$.data[0].enabled").value(true));
    }

    @Test
    @DisplayName("POST /api/anomaly-thresholds は 201 と作成された閾値設定を返すこと")
    void create_returns201() throws Exception {
        given(thresholdService.create(eq(UUID.fromString(USER_ID)), any())).willReturn(sampleResponse());

        mvc.perform(post("/api/anomaly-thresholds")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric_type\":\"temperature\",\"min_value\":10.0,\"max_value\":35.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.metric_type").value("temperature"))
                .andExpect(jsonPath("$.data.id").value(THRESHOLD_ID));
    }

    @Test
    @DisplayName("POST /api/anomaly-thresholds で metric_type が不正な場合 400 を返すこと")
    void create_invalidMetricType_returns400() throws Exception {
        mvc.perform(post("/api/anomaly-thresholds")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric_type\":\"invalid\",\"max_value\":35.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/anomaly-thresholds で min_value と max_value の両方が省略された場合 400 を返すこと")
    void create_bothValuesAbsent_returns400() throws Exception {
        mvc.perform(post("/api/anomaly-thresholds")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric_type\":\"temperature\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/anomaly-thresholds で power_w かつ max_value が省略された場合 400 を返すこと")
    void create_powerW_maxValueAbsent_returns400() throws Exception {
        mvc.perform(post("/api/anomaly-thresholds")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric_type\":\"power_w\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/anomaly-thresholds で同一 metric_type が存在する場合 409 を返すこと")
    void create_duplicateMetricType_returns409() throws Exception {
        given(thresholdService.create(eq(UUID.fromString(USER_ID)), any()))
                .willThrow(new DuplicateResourceException("同一の metric_type の閾値設定が既に存在します"));

        mvc.perform(post("/api/anomaly-thresholds")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metric_type\":\"temperature\",\"max_value\":35.0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("PATCH /api/anomaly-thresholds/{id} は 200 と更新された閾値設定を返すこと")
    void update_returns200() throws Exception {
        AnomalyThresholdResponse updated = new AnomalyThresholdResponse(
                UUID.fromString(THRESHOLD_ID), "temperature",
                new BigDecimal("10.0"), new BigDecimal("40.0"),
                false,
                LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0, 0));
        given(thresholdService.update(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(THRESHOLD_ID)), any()))
                .willReturn(updated);

        mvc.perform(patch("/api/anomaly-thresholds/" + THRESHOLD_ID)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"max_value\":40.0,\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.max_value").value(40.0))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("PATCH /api/anomaly-thresholds/{id} で閾値設定が存在しない場合 404 を返すこと")
    void update_notFound_returns404() throws Exception {
        given(thresholdService.update(any(), any(), any()))
                .willThrow(new ResourceNotFoundException("閾値設定が見つかりません"));

        mvc.perform(patch("/api/anomaly-thresholds/" + THRESHOLD_ID)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/anomaly-thresholds/{id} は 204 を返しサービスを呼び出すこと")
    void delete_returns204() throws Exception {
        willDoNothing().given(thresholdService).delete(UUID.fromString(USER_ID), UUID.fromString(THRESHOLD_ID));

        mvc.perform(delete("/api/anomaly-thresholds/" + THRESHOLD_ID).session(session()))
                .andExpect(status().isNoContent());

        then(thresholdService).should().delete(UUID.fromString(USER_ID), UUID.fromString(THRESHOLD_ID));
    }

    @Test
    @DisplayName("DELETE /api/anomaly-thresholds/{id} で閾値設定が存在しない場合 404 を返すこと")
    void delete_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("閾値設定が見つかりません"))
                .given(thresholdService).delete(any(), any());

        mvc.perform(delete("/api/anomaly-thresholds/" + THRESHOLD_ID).session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
