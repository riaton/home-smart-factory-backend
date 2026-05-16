package com.example.smartfactory.worker.anomaly;

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
import java.util.UUID;

@Entity
@Table(name = "anomaly_thresholds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnomalyThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "min_value", precision = 8, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 8, scale = 2)
    private BigDecimal maxValue;

    @Column(nullable = false)
    private boolean enabled;
}
