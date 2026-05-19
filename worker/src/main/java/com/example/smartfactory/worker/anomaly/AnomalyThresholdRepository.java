package com.example.smartfactory.worker.anomaly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnomalyThresholdRepository extends JpaRepository<AnomalyThreshold, UUID> {

    List<AnomalyThreshold> findByUserIdAndEnabledTrue(UUID userId);
}
