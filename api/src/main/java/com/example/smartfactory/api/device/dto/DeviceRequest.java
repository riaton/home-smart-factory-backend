package com.example.smartfactory.api.device.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeviceRequest(

        @JsonProperty("device_id")
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-]+$")
        @Size(max = 100)
        String deviceId,

        @Size(max = 255)
        String name) {
}
