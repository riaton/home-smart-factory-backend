package com.example.smartfactory.api.threshold.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AnomalyThresholdUpdateRequest(

        @JsonProperty("min_value")
        BigDecimal minValue,

        @JsonProperty("max_value")
        BigDecimal maxValue,

        Boolean enabled) {
}
