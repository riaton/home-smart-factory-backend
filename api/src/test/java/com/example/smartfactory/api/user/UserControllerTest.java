package com.example.smartfactory.api.user;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.api.user.dto.UserResponse;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @Mock
    private UserService userService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/users/me は 200 とユーザー情報を返すこと")
    void getMe_returns200WithUserData() throws Exception {
        UserResponse response = new UserResponse(
                UUID.fromString(USER_ID),
                "user@example.com",
                LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(userService.getMe(UUID.fromString(USER_ID))).willReturn(response);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);

        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.created_at").exists());
    }

    @Test
    @DisplayName("GET /api/users/me でユーザーが存在しない場合 404 を返すこと")
    void getMe_userNotFound_returns404() throws Exception {
        given(userService.getMe(any())).willThrow(new ResourceNotFoundException("ユーザーが見つかりません"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);

        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/users/me は 204 を返しサービスを呼び出すこと")
    void deleteMe_returns204() throws Exception {
        willDoNothing().given(userService).deleteMe(UUID.fromString(USER_ID));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);

        mvc.perform(delete("/api/users/me").session(session))
                .andExpect(status().isNoContent());

        then(userService).should().deleteMe(UUID.fromString(USER_ID));
    }
}
