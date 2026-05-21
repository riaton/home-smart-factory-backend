package com.example.smartfactory.batch.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IotDataBatchRepository extends JpaRepository<IotData, Long> {

    @Query(nativeQuery = true, value = """
            SELECT
                AVG(temperature)                                 AS avg_temperature,
                AVG(humidity)                                    AS avg_humidity,
                SUM(power_w) / 60.0 / 1000.0                    AS total_power_kwh,
                SUM(CASE WHEN motion = 1 THEN 1 ELSE 0 END)     AS total_motion_minutes
            FROM iot_data
            WHERE user_id = :userId
              AND recorded_at >= :from
              AND recorded_at < :to
            """)
    IotDataAggregate findAggregateByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM iot_data
            WHERE id IN (
                SELECT id FROM iot_data
                WHERE recorded_at < :cutoff
                LIMIT 10000
            )
            """)
    int deleteOldDataBatch(@Param("cutoff") OffsetDateTime cutoff);
}
