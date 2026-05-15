package com.example.smartfactory.api.config;

import com.example.smartfactory.api.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SessionAuthInterceptorTest {

    private SessionAuthInterceptor interceptor;

    @BeforeEach
    void setup() {
        interceptor = new SessionAuthInterceptor(new ObjectMapper());
    }

    @Test
    @DisplayName("セッションに user_id がある場合は通過すること")
    void preHandle_withValidSession_returnsTrue() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_USER_ID, "user-uuid");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("セッションがない場合は 401 を返すこと")
    void preHandle_withNoSession_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }

    @Test
    @DisplayName("セッションはあるが user_id がない場合は 401 を返すこと")
    void preHandle_withSessionButNoUserId_returns401() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
    }
}
