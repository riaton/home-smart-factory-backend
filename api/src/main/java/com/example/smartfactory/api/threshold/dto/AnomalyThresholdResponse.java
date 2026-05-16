package com.example.smartfactory.api.threshold.dto;

import com.example.smartfactory.api.threshold.AnomalyThreshold;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AnomalyThresholdResponse(

        UUID id,

        @JsonProperty("metric_type") String metricType,

        @JsonProperty("min_value") BigDecimal minValue,

        @JsonProperty("max_value") BigDecimal maxValue,

        boolean enabled,

        @JsonProperty("created_at") LocalDateTime createdAt,

        @JsonProperty("updated_at") LocalDateTime updatedAt) {

    public static AnomalyThresholdResponse from(AnomalyThreshold threshold) {
        return new AnomalyThresholdResponse(
                threshold.getId(),
                threshold.getMetricType(),
                threshold.getMinValue(),
                threshold.getMaxValue(),
                threshold.isEnabled(),
                threshold.getCreatedAt(),
                threshold.getUpdatedAt());
    }
}
