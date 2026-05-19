package com.example.smartfactory.api.anomalylog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AnomalyLogRepository extends JpaRepository<AnomalyLog, Long> {

    @Query(value = "SELECT a FROM AnomalyLog a WHERE a.userId = :userId "
            + "AND (:deviceId IS NULL OR a.deviceId = :deviceId) "
            + "AND (:metricType IS NULL OR a.metricType = :metricType) "
            + "AND (:from IS NULL OR a.detectedAt >= :from) "
            + "AND (:to IS NULL OR a.detectedAt <= :to) "
            + "ORDER BY a.detectedAt DESC",
           countQuery = "SELECT COUNT(a) FROM AnomalyLog a WHERE a.userId = :userId "
            + "AND (:deviceId IS NULL OR a.deviceId = :deviceId) "
            + "AND (:metricType IS NULL OR a.metricType = :metricType) "
            + "AND (:from IS NULL OR a.detectedAt >= :from) "
            + "AND (:to IS NULL OR a.detectedAt <= :to)")
    Page<AnomalyLog> findByFilter(
            @Param("userId") UUID userId,
            @Param("deviceId") String deviceId,
            @Param("metricType") String metricType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
