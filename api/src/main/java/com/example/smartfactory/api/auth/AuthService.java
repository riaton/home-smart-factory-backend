package com.example.smartfactory.api.auth;

import com.example.smartfactory.api.auth.dto.GoogleTokenResponse;
import com.example.smartfactory.api.auth.dto.GoogleUserInfoResponse;
import com.example.smartfactory.api.user.User;
import com.example.smartfactory.api.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_KEY_OAUTH_STATE = "oauth_state";

    public static final String SESSION_KEY_USER_ID = "user_id";

    private static final String GOOGLE_AUTH_URL =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private final GoogleOAuthClient oauthClient;

    private final UserRepository userRepository;

    private final GoogleOAuthProperties properties;

    public String buildAuthorizationUrl(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(SESSION_KEY_OAUTH_STATE, state);
        return UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "email openid")
                .queryParam("state", state)
                .build().toUriString();
    }

    @Transactional
    public void handleCallback(String code, String state, HttpSession session) {
        String savedState = (String) session.getAttribute(SESSION_KEY_OAUTH_STATE);
        if (savedState == null || !savedState.equals(state)) {
            throw new InvalidStateException();
        }
        session.removeAttribute(SESSION_KEY_OAUTH_STATE);

        GoogleTokenResponse tokenResponse = oauthClient.exchangeCode(code);
        GoogleUserInfoResponse userInfo = oauthClient.getUserInfo(tokenResponse.accessToken());

        User user = userRepository.findByGoogleId(userInfo.sub())
                .orElseGet(() -> userRepository.save(User.create(userInfo.sub(), userInfo.email())));

        session.setAttribute(SESSION_KEY_USER_ID, user.getId().toString());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
