package com.example.smartfactory.api.anomalylog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "anomaly_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnomalyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "threshold_value", precision = 8, scale = 2)
    private BigDecimal thresholdValue;

    @Column(name = "actual_value", precision = 8, scale = 2)
    private BigDecimal actualValue;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
