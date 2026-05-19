package com.example.smartfactory.worker.anomaly;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyLogRepository extends JpaRepository<AnomalyLog, Long> {
}
