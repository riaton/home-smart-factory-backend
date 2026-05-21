package com.example.smartfactory.api.report.dto;

import com.example.smartfactory.api.report.DailyReport;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReportDetailResponse(

        UUID id,

        @JsonProperty("report_date") LocalDate reportDate,

        @JsonProperty("total_power_kwh") BigDecimal totalPowerKwh,

        @JsonProperty("avg_temperature") BigDecimal avgTemperature,

        @JsonProperty("avg_humidity") BigDecimal avgHumidity,

        @JsonProperty("total_motion_minutes") Integer totalMotionMinutes,

        @JsonProperty("anomaly_count") int anomalyCount,

        @JsonProperty("anomaly_summary") List<AnomalySummaryItem> anomalySummary,

        @JsonProperty("download_count_today") int downloadCountToday,

        @JsonProperty("created_at") OffsetDateTime createdAt) {

    public static ReportDetailResponse from(
            DailyReport report,
            List<AnomalySummaryItem> anomalySummary,
            int downloadCountToday) {
        return new ReportDetailResponse(
                report.getId(),
                report.getReportDate(),
                report.getTotalPowerKwh(),
                report.getAvgTemperature(),
                report.getAvgHumidity(),
                report.getTotalMotionMinutes(),
                report.getAnomalyCount(),
                anomalySummary,
                downloadCountToday,
                report.getCreatedAt());
    }
}
