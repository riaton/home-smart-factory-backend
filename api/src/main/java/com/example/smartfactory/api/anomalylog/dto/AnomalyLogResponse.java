package com.example.smartfactory.api.anomalylog.dto;

import com.example.smartfactory.api.anomalylog.AnomalyLog;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AnomalyLogResponse(

        long id,

        @JsonProperty("device_id") String deviceId,

        @JsonProperty("metric_type") String metricType,

        @JsonProperty("threshold_value") BigDecimal thresholdValue,

        @JsonProperty("actual_value") BigDecimal actualValue,

        String message,

        @JsonProperty("detected_at") OffsetDateTime detectedAt) {

    public static AnomalyLogResponse from(AnomalyLog anomalyLog) {
        return new AnomalyLogResponse(
                anomalyLog.getId(),
                anomalyLog.getDeviceId(),
                anomalyLog.getMetricType(),
                anomalyLog.getThresholdValue(),
                anomalyLog.getActualValue(),
                anomalyLog.getMessage(),
                anomalyLog.getDetectedAt());
    }
}
