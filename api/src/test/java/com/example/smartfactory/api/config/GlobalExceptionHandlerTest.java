package com.example.smartfactory.api.config;

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
    @DisplayName("ResourceNotFoundException は 404 と error フィールドを返すこと")
    void handleNotFound_returns404WithErrorField() throws Exception {
        given(healthController.health()).willThrow(new ResourceNotFoundException("デバイスが見つかりません"));

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("デバイスが見つかりません"));
    }

    @Test
    @DisplayName("DuplicateResourceException は 409 と error フィールドを返すこと")
    void handleConflict_returns409WithErrorField() throws Exception {
        given(healthController.health()).willThrow(new DuplicateResourceException("デバイスIDが重複しています"));

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("デバイスIDが重複しています"));
    }

    @Test
    @DisplayName("DownloadLimitExceededException は 429 と error フィールドを返すこと")
    void handleTooManyRequests_returns429WithErrorField() throws Exception {
        given(healthController.health()).willThrow(new DownloadLimitExceededException());

        mvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
