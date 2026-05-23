package com.example.smartfactory.api.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AnomalySummaryItem(

        @JsonProperty("device_id") String deviceId,

        @JsonProperty("metric_type") String metricType,

        long count,

        @JsonProperty("max_value") BigDecimal maxValue) {
}
