package com.example.smartfactory.batch.cleanup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface ReportDownloadCleanupRepository extends JpaRepository<ReportDownload, Long> {

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM report_downloads WHERE download_date < :cutoff")
    int deleteByDownloadDateBefore(@Param("cutoff") LocalDate cutoff);
}
