package com.example.smartfactory.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGoogleId(String googleId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM report_downloads WHERE user_id = :userId", nativeQuery = true)
    void deleteReportDownloadsByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM daily_reports WHERE user_id = :userId", nativeQuery = true)
    void deleteDailyReportsByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM anomaly_logs WHERE user_id = :userId", nativeQuery = true)
    void deleteAnomalyLogsByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM anomaly_thresholds WHERE user_id = :userId", nativeQuery = true)
    void deleteAnomalyThresholdsByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM iot_data WHERE user_id = :userId", nativeQuery = true)
    void deleteIotDataByUserId(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM devices WHERE user_id = :userId", nativeQuery = true)
    void deleteDevicesByUserId(@Param("userId") UUID userId);
}
