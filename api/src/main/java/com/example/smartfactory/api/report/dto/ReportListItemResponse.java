package com.example.smartfactory.api.report.dto;

import com.example.smartfactory.api.report.DailyReport;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportListItemResponse(

        UUID id,

        @JsonProperty("report_date") LocalDate reportDate,

        @JsonProperty("total_power_kwh") BigDecimal totalPowerKwh,

        @JsonProperty("avg_temperature") BigDecimal avgTemperature,

        @JsonProperty("avg_humidity") BigDecimal avgHumidity,

        @JsonProperty("total_motion_minutes") Integer totalMotionMinutes,

        @JsonProperty("anomaly_count") int anomalyCount,

        @JsonProperty("created_at") OffsetDateTime createdAt) {

    public static ReportListItemResponse from(DailyReport report) {
        return new ReportListItemResponse(
                report.getId(),
                report.getReportDate(),
                report.getTotalPowerKwh(),
                report.getAvgTemperature(),
                report.getAvgHumidity(),
                report.getTotalMotionMinutes(),
                report.getAnomalyCount(),
                report.getCreatedAt());
    }
}
