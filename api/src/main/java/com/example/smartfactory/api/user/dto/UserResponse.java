package com.example.smartfactory.api.user.dto;

import com.example.smartfactory.api.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(

        UUID id,

        String email,

        @JsonProperty("created_at") LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
