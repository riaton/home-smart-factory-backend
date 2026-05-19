package com.example.smartfactory.worker.iotdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IotMessagePayload(

        @JsonProperty("device_id") String deviceId,

        BigDecimal temperature,

        BigDecimal humidity,

        Integer motion,

        @JsonProperty("power_w") BigDecimal powerW,

        @JsonProperty("recorded_at") OffsetDateTime recordedAt) {
}
