package com.example.smartfactory.api.threshold;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnomalyThresholdRepository extends JpaRepository<AnomalyThreshold, UUID> {

    List<AnomalyThreshold> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<AnomalyThreshold> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndMetricType(UUID userId, String metricType);
}
