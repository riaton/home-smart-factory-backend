package com.example.smartfactory.api.iotdata;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.api.iotdata.dto.IotDataResponse;
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
class IotDataControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @Mock
    private IotDataService iotDataService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new IotDataController(iotDataService))
                .setConversionService(new DefaultFormattingConversionService())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    private PagedResponse<IotDataResponse> sampleResponse(int page, int perPage) {
        IotDataResponse item = new IotDataResponse(
                1L, "room01",
                new BigDecimal("25.3"), new BigDecimal("60.1"), 1, new BigDecimal("120.5"),
                OffsetDateTime.parse("2026-01-15T10:00:00+09:00"));
        return new PagedResponse<>(List.of(item), new PagedResponse.Pagination(1, page, perPage));
    }

    @Test
    @DisplayName("GET /api/iot-data は 200 とデータ一覧・ページネーションを返すこと")
    void getIotData_returns200WithData() throws Exception {
        given(iotDataService.findAll(
                eq(UUID.fromString(USER_ID)), isNull(), any(), any(), eq(1), eq(100)))
                .willReturn(sampleResponse(1, 100));

        mvc.perform(get("/api/iot-data")
                        .session(session())
                        .param("from", "2026-01-15T00:00:00+09:00")
                        .param("to", "2026-01-15T23:59:59+09:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].device_id").value("room01"))
                .andExpect(jsonPath("$.data[0].temperature").value(25.3))
                .andExpect(jsonPath("$.data[0].motion").value(1))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.per_page").value(100));
    }

    @Test
    @DisplayName("device_id パラメータ指定時はサービスに渡されること")
    void getIotData_withDeviceId_passesToService() throws Exception {
        given(iotDataService.findAll(
                eq(UUID.fromString(USER_ID)), eq("room01"), any(), any(), eq(1), eq(100)))
                .willReturn(sampleResponse(1, 100));

        mvc.perform(get("/api/iot-data")
                        .session(session())
                        .param("device_id", "room01")
                        .param("from", "2026-01-15T00:00:00+09:00")
                        .param("to", "2026-01-15T23:59:59+09:00"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("page, per_page パラメータが指定時はサービスに渡されること")
    void getIotData_withPagination_passesToService() throws Exception {
        given(iotDataService.findAll(
                eq(UUID.fromString(USER_ID)), isNull(), any(), any(), eq(2), eq(50)))
                .willReturn(sampleResponse(2, 50));

        mvc.perform(get("/api/iot-data")
                        .session(session())
                        .param("from", "2026-01-15T00:00:00+09:00")
                        .param("to", "2026-01-15T23:59:59+09:00")
                        .param("page", "2")
                        .param("per_page", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(2))
                .andExpect(jsonPath("$.pagination.per_page").value(50));
    }

    @Test
    @DisplayName("from パラメータが欠如している場合 400 を返すこと")
    void getIotData_missingFrom_returns400() throws Exception {
        mvc.perform(get("/api/iot-data")
                        .session(session())
                        .param("to", "2026-01-15T23:59:59+09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("to パラメータが欠如している場合 400 を返すこと")
    void getIotData_missingTo_returns400() throws Exception {
        mvc.perform(get("/api/iot-data")
                        .session(session())
                        .param("from", "2026-01-15T00:00:00+09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
