package com.example.smartfactory.batch.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AnomalyLogBatchRepository extends JpaRepository<AnomalyLog, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                device_id,
                metric_type,
                COUNT(*)            AS count,
                MAX(actual_value)   AS max_actual_value
            FROM anomaly_logs
            WHERE user_id = :userId
              AND detected_at >= :from
              AND detected_at < :to
            GROUP BY device_id, metric_type
            """)
    List<AnomalyGroupResult> findGroupedByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM anomaly_logs WHERE detected_at < :cutoff")
    int deleteByDetectedAtBefore(@Param("cutoff") OffsetDateTime cutoff);
}
