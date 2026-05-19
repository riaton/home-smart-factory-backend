package com.example.smartfactory.api.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Device> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM anomaly_logs WHERE device_id = :deviceId", nativeQuery = true)
    void deleteAnomalyLogsByDeviceId(@Param("deviceId") String deviceId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM iot_data WHERE device_id = :deviceId", nativeQuery = true)
    void deleteIotDataByDeviceId(@Param("deviceId") String deviceId);
}
