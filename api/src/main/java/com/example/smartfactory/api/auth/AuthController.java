package com.example.smartfactory.api.auth;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final GoogleOAuthProperties properties;

    @GetMapping("/google")
    public ResponseEntity<Void> redirectToGoogle(HttpSession session) {
        String url = authService.buildAuthorizationUrl(session);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session) {
        if (error != null || code == null || state == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(properties.frontendUrl() + "/login?error=oauth_failed"))
                    .build();
        }
        authService.handleCallback(code, state, session);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(properties.frontendUrl()))
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.noContent().build();
    }
}
