package com.example.smartfactory.api.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyReport {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_power_kwh", precision = 10, scale = 4)
    private BigDecimal totalPowerKwh;

    @Column(name = "avg_temperature", precision = 5, scale = 2)
    private BigDecimal avgTemperature;

    @Column(name = "avg_humidity", precision = 5, scale = 2)
    private BigDecimal avgHumidity;

    @Column(name = "total_motion_minutes")
    private Integer totalMotionMinutes;

    @Column(name = "anomaly_count", nullable = false)
    private int anomalyCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anomaly_summary")
    private String anomalySummary;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
