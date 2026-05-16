package com.example.smartfactory.api.device;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.api.device.dto.DeviceResponse;
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
class DeviceControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    private static final String DEVICE_ID = "00000000-0000-0000-0000-000000000002";

    @Mock
    private DeviceService deviceService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    @Test
    @DisplayName("GET /api/devices は 200 とデバイス一覧を返すこと")
    void getDevices_returns200WithList() throws Exception {
        DeviceResponse response = new DeviceResponse(
                UUID.fromString(DEVICE_ID), "room01", "リビング",
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceService.getDevices(UUID.fromString(USER_ID))).willReturn(List.of(response));

        mvc.perform(get("/api/devices").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].device_id").value("room01"))
                .andExpect(jsonPath("$.data[0].name").value("リビング"));
    }

    @Test
    @DisplayName("POST /api/devices は 201 と登録デバイスを返すこと")
    void createDevice_returns201() throws Exception {
        DeviceResponse response = new DeviceResponse(
                UUID.fromString(DEVICE_ID), "room01", "リビング",
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceService.createDevice(eq(UUID.fromString(USER_ID)), any())).willReturn(response);

        mvc.perform(post("/api/devices")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"device_id\":\"room01\",\"name\":\"リビング\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.device_id").value("room01"))
                .andExpect(jsonPath("$.data.id").value(DEVICE_ID));
    }

    @Test
    @DisplayName("POST /api/devices で device_id が重複した場合 409 を返すこと")
    void createDevice_duplicateDeviceId_returns409() throws Exception {
        given(deviceService.createDevice(eq(UUID.fromString(USER_ID)), any()))
                .willThrow(new DuplicateResourceException("device_id が既に登録済みです"));

        mvc.perform(post("/api/devices")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"device_id\":\"room01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("POST /api/devices でバリデーションエラーの場合 400 を返すこと")
    void createDevice_invalidRequest_returns400() throws Exception {
        mvc.perform(post("/api/devices")
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"device_id\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /api/devices/{id} は 200 と更新デバイスを返すこと")
    void updateDevice_returns200() throws Exception {
        DeviceResponse response = new DeviceResponse(
                UUID.fromString(DEVICE_ID), "room01", "寝室",
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceService.updateDevice(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(DEVICE_ID)), any()))
                .willReturn(response);

        mvc.perform(patch("/api/devices/" + DEVICE_ID)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"寝室\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("寝室"));
    }

    @Test
    @DisplayName("PATCH /api/devices/{id} でデバイスが存在しない場合 404 を返すこと")
    void updateDevice_deviceNotFound_returns404() throws Exception {
        given(deviceService.updateDevice(any(), any(), any()))
                .willThrow(new ResourceNotFoundException("デバイスが見つかりません"));

        mvc.perform(patch("/api/devices/" + DEVICE_ID)
                        .session(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"寝室\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/devices/{id} は 204 を返しサービスを呼び出すこと")
    void deleteDevice_returns204() throws Exception {
        willDoNothing().given(deviceService).deleteDevice(UUID.fromString(USER_ID), UUID.fromString(DEVICE_ID));

        mvc.perform(delete("/api/devices/" + DEVICE_ID).session(session()))
                .andExpect(status().isNoContent());

        then(deviceService).should().deleteDevice(UUID.fromString(USER_ID), UUID.fromString(DEVICE_ID));
    }

    @Test
    @DisplayName("DELETE /api/devices/{id} でデバイスが存在しない場合 404 を返すこと")
    void deleteDevice_deviceNotFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("デバイスが見つかりません"))
                .given(deviceService).deleteDevice(any(), any());

        mvc.perform(delete("/api/devices/" + DEVICE_ID).session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
