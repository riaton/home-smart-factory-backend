package com.example.smartfactory.api.config;

import com.example.smartfactory.api.auth.InvalidStateException;
import com.example.smartfactory.api.health.HealthController;
import com.example.smartfactory.common.exception.DownloadLimitExceededException;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HealthController healthController;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(healthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException は 404 と error.code/message を返すこと")
    void handleNotFound_returns404() throws Exception {
        given(healthController.health()).willThrow(new ResourceNotFoundException("デバイスが見つかりません"));

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("デバイスが見つかりません"));
    }

    @Test
    @DisplayName("DuplicateResourceException は 409 と error.code/message を返すこと")
    void handleConflict_returns409() throws Exception {
        given(healthController.health()).willThrow(new DuplicateResourceException("デバイスIDが重複しています"));

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.error.message").value("デバイスIDが重複しています"));
    }

    @Test
    @DisplayName("DownloadLimitExceededException は 429 と error.code を返すこと")
    void handleTooManyRequests_returns429() throws Exception {
        given(healthController.health()).willThrow(new DownloadLimitExceededException());

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("DOWNLOAD_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("InvalidStateException は 400 と error.code INVALID_STATE を返すこと")
    void handleInvalidState_returns400() throws Exception {
        given(healthController.health()).willThrow(new InvalidStateException());

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    @DisplayName("IllegalArgumentException は 400 と VALIDATION_ERROR を返すこと")
    void handleIllegalArgument_returns400() throws Exception {
        given(healthController.health()).willThrow(new IllegalArgumentException("page は1以上を指定してください"));

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("page は1以上を指定してください"));
    }
}
