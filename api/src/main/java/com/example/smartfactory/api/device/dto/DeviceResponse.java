package com.example.smartfactory.api.device.dto;

import com.example.smartfactory.api.device.Device;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceResponse(

        UUID id,

        @JsonProperty("device_id") String deviceId,

        String name,

        @JsonProperty("created_at") LocalDateTime createdAt) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceId(),
                device.getName(),
                device.getCreatedAt());
    }
}
