package com.example.smartfactory.worker.iotdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface IotDataRepository extends JpaRepository<IotData, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO iot_data "
            + "(device_id, user_id, temperature, humidity, motion, power_w, recorded_at) "
            + "VALUES (:deviceId, :userId, :temperature, :humidity, :motion, :powerW, :recordedAt) "
            + "ON CONFLICT (device_id, recorded_at) DO NOTHING",
            nativeQuery = true)
    void insertWithOnConflictDoNothing(
            @Param("deviceId") String deviceId,
            @Param("userId") UUID userId,
            @Param("temperature") BigDecimal temperature,
            @Param("humidity") BigDecimal humidity,
            @Param("motion") Integer motion,
            @Param("powerW") BigDecimal powerW,
            @Param("recordedAt") OffsetDateTime recordedAt);
}
