package com.example.smartfactory.api.auth;

import com.example.smartfactory.api.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String FRONTEND_URL = "http://localhost:3000";

    @Mock
    private AuthService authService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "test-client-id", "test-client-secret",
                "http://localhost:8080/auth/google/callback", FRONTEND_URL);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, properties))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /auth/google は Google 認可URLへ302リダイレクトすること")
    void redirectToGoogle_returns302() throws Exception {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=test";
        given(authService.buildAuthorizationUrl(any())).willReturn(googleAuthUrl);

        mvc.perform(get("/auth/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", googleAuthUrl));
    }

    @Test
    @DisplayName("GET /auth/google/callback に code と state が揃っていればフロントエンドへリダイレクトすること")
    void handleCallback_validParams_redirectsToFrontend() throws Exception {
        willDoNothing().given(authService).handleCallback(eq("code123"), eq("state456"), any());

        mvc.perform(get("/auth/google/callback")
                        .param("code", "code123")
                        .param("state", "state456"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FRONTEND_URL));
    }

    @Test
    @DisplayName("GET /auth/google/callback に error パラメータがあればエラーページへリダイレクトすること")
    void handleCallback_withError_redirectsToErrorPage() throws Exception {
        mvc.perform(get("/auth/google/callback")
                        .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FRONTEND_URL + "/login?error=oauth_failed"));
    }

    @Test
    @DisplayName("GET /auth/google/callback に code がなければエラーページへリダイレクトすること")
    void handleCallback_missingCode_redirectsToErrorPage() throws Exception {
        mvc.perform(get("/auth/google/callback")
                        .param("state", "state456"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FRONTEND_URL + "/login?error=oauth_failed"));
    }

    @Test
    @DisplayName("GET /auth/google/callback に state がなければエラーページへリダイレクトすること")
    void handleCallback_missingState_redirectsToErrorPage() throws Exception {
        mvc.perform(get("/auth/google/callback")
                        .param("code", "code123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FRONTEND_URL + "/login?error=oauth_failed"));
    }

    @Test
    @DisplayName("handleCallback で InvalidStateException がスローされると 400 を返すこと")
    void handleCallback_invalidState_returns400() throws Exception {
        willThrow(new InvalidStateException())
                .given(authService).handleCallback(any(), any(), any());

        mvc.perform(get("/auth/google/callback")
                        .param("code", "code123")
                        .param("state", "wrong-state"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    @DisplayName("POST /auth/logout は204を返しセッションを無効化すること")
    void logout_returns204() throws Exception {
        MockHttpSession session = new MockHttpSession();
        willDoNothing().given(authService).logout(any());

        mvc.perform(post("/auth/logout").session(session))
                .andExpect(status().isNoContent());

        then(authService).should().logout(any());
    }
}
