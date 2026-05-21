package com.example.smartfactory.api.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_downloads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "download_date", nullable = false)
    private LocalDate downloadDate;

    @Column(name = "downloaded_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime downloadedAt;

    public static ReportDownload create(UUID userId, UUID reportId, LocalDate downloadDate) {
        ReportDownload d = new ReportDownload();
        d.userId = userId;
        d.reportId = reportId;
        d.downloadDate = downloadDate;
        return d;
    }
}
