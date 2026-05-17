package com.example.smartfactory.api.device.dto;

import jakarta.validation.constraints.Size;

public record DeviceUpdateRequest(

        @Size(max = 255)
        String name) {
}
