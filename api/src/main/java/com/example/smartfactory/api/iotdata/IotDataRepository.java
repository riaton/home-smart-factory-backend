package com.example.smartfactory.api.iotdata;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface IotDataRepository extends JpaRepository<IotData, Long> {

    @Query(value = "SELECT d FROM IotData d WHERE d.userId = :userId "
            + "AND (:deviceId IS NULL OR d.deviceId = :deviceId) "
            + "AND d.recordedAt >= :from AND d.recordedAt <= :to "
            + "ORDER BY d.recordedAt ASC",
           countQuery = "SELECT COUNT(d) FROM IotData d WHERE d.userId = :userId "
            + "AND (:deviceId IS NULL OR d.deviceId = :deviceId) "
            + "AND d.recordedAt >= :from AND d.recordedAt <= :to")
    Page<IotData> findByFilter(
            @Param("userId") UUID userId,
            @Param("deviceId") String deviceId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
