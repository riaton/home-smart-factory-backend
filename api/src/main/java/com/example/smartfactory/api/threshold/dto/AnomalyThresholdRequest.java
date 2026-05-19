package com.example.smartfactory.api.threshold.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record AnomalyThresholdRequest(

        @JsonProperty("metric_type")
        @NotBlank
        @Pattern(regexp = "temperature|humidity|power_w")
        String metricType,

        @JsonProperty("min_value")
        BigDecimal minValue,

        @JsonProperty("max_value")
        BigDecimal maxValue) {

    @AssertTrue(message = "min_value と max_value の少なくともどちらか一方は必須です")
    public boolean isAtLeastOneValueProvided() {
        if ("power_w".equals(metricType)) {
            return maxValue != null;
        }
        return minValue != null || maxValue != null;
    }
}
