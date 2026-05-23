package com.example.smartfactory.api.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Page<DailyReport> findByUserIdOrderByReportDateDesc(UUID userId, Pageable pageable);

    Optional<DailyReport> findByIdAndUserId(UUID id, UUID userId);
}
