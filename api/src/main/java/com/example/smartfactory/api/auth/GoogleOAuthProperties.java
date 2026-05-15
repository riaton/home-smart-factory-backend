package com.example.smartfactory.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.oauth")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendUrl
) {}
