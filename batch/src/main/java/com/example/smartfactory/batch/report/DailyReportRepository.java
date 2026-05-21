package com.example.smartfactory.batch.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    boolean existsByUserIdAndReportDate(UUID userId, LocalDate reportDate);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM daily_reports WHERE report_date < :cutoff")
    int deleteByReportDateBefore(@Param("cutoff") LocalDate cutoff);
}
