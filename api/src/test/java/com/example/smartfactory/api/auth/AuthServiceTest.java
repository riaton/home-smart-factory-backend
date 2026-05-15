package com.example.smartfactory.api.auth;

import com.example.smartfactory.api.auth.dto.GoogleTokenResponse;
import com.example.smartfactory.api.auth.dto.GoogleUserInfoResponse;
import com.example.smartfactory.api.user.User;
import com.example.smartfactory.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleOAuthClient oauthClient;

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setup() {
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "test-client-id", "test-client-secret",
                "http://localhost:8080/auth/google/callback", "http://localhost:3000");
        authService = new AuthService(oauthClient, userRepository, properties);
    }

    @Test
    @DisplayName("buildAuthorizationUrl はセッションに state を保存し Google 認可URLを返すこと")
    void buildAuthorizationUrl_savesStateAndReturnsUrl() {
        MockHttpSession session = new MockHttpSession();

        String url = authService.buildAuthorizationUrl(session);

        assertThat(session.getAttribute(AuthService.SESSION_KEY_OAUTH_STATE)).isNotNull();
        assertThat(url).contains("accounts.google.com");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("response_type=code");
    }

    @Test
    @DisplayName("handleCallback は有効な state でユーザーをセッションに保存すること")
    void handleCallback_validState_savesUserIdInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_OAUTH_STATE, "valid-state");

        GoogleTokenResponse token = new GoogleTokenResponse("access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse("google-sub-123", "user@example.com");
        User user = mock(User.class);
        given(user.getId()).willReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        given(oauthClient.exchangeCode("auth-code")).willReturn(token);
        given(oauthClient.getUserInfo("access-token")).willReturn(userInfo);
        given(userRepository.findByGoogleId("google-sub-123")).willReturn(Optional.of(user));

        authService.handleCallback("auth-code", "valid-state", session);

        assertThat(session.getAttribute(AuthService.SESSION_KEY_USER_ID)).isNotNull();
        assertThat(session.getAttribute(AuthService.SESSION_KEY_OAUTH_STATE)).isNull();
    }

    @Test
    @DisplayName("handleCallback は state 不一致のとき InvalidStateException をスローすること")
    void handleCallback_invalidState_throwsInvalidStateException() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_OAUTH_STATE, "expected-state");

        assertThatThrownBy(() -> authService.handleCallback("auth-code", "wrong-state", session))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("handleCallback はセッションに state がないとき InvalidStateException をスローすること")
    void handleCallback_noStateInSession_throwsInvalidStateException() {
        MockHttpSession session = new MockHttpSession();

        assertThatThrownBy(() -> authService.handleCallback("auth-code", "some-state", session))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("handleCallback は新規ユーザーのとき UserRepository に保存すること")
    void handleCallback_newUser_savesUserToRepository() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_OAUTH_STATE, "valid-state");

        GoogleTokenResponse token = new GoogleTokenResponse("access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse("new-google-sub", "new@example.com");
        User savedUser = mock(User.class);
        given(savedUser.getId()).willReturn(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        given(oauthClient.exchangeCode("auth-code")).willReturn(token);
        given(oauthClient.getUserInfo("access-token")).willReturn(userInfo);
        given(userRepository.findByGoogleId("new-google-sub")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        authService.handleCallback("auth-code", "valid-state", session);

        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("logout はセッションを無効化すること")
    void logout_invalidatesSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_KEY_USER_ID, "user-uuid");

        authService.logout(session);

        assertThat(session.isInvalid()).isTrue();
    }
}
