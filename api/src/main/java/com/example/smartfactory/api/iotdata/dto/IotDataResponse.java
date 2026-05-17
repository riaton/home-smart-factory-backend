package com.example.smartfactory.api.iotdata.dto;

import com.example.smartfactory.api.iotdata.IotData;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IotDataResponse(

        long id,

        @JsonProperty("device_id") String deviceId,

        BigDecimal temperature,

        BigDecimal humidity,

        Integer motion,

        @JsonProperty("power_w") BigDecimal powerW,

        @JsonProperty("recorded_at") OffsetDateTime recordedAt) {

    public static IotDataResponse from(IotData iotData) {
        return new IotDataResponse(
                iotData.getId(),
                iotData.getDeviceId(),
                iotData.getTemperature(),
                iotData.getHumidity(),
                iotData.getMotion(),
                iotData.getPowerW(),
                iotData.getRecordedAt());
    }
}
