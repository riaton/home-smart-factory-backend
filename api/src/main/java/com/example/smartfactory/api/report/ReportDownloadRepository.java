package com.example.smartfactory.api.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReportDownloadRepository extends JpaRepository<ReportDownload, Long> {

    long countByUserIdAndDownloadDate(UUID userId, LocalDate downloadDate);
}
