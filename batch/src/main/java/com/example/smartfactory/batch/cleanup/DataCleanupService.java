package com.example.smartfactory.batch.cleanup;

import com.example.smartfactory.batch.report.AnomalyLogBatchRepository;
import com.example.smartfactory.batch.report.DailyReportRepository;
import com.example.smartfactory.batch.report.IotDataBatchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(DataCleanupService.class);

    private static final int IOT_DATA_RETENTION_DAYS = 90;

    private static final int LONG_TERM_RETENTION_YEARS = 1;

    private final IotDataBatchRepository iotDataBatchRepository;

    private final AnomalyLogBatchRepository anomalyLogBatchRepository;

    private final ReportDownloadCleanupRepository reportDownloadCleanupRepository;

    private final DailyReportRepository dailyReportRepository;

    public void cleanupOldData() {
        OffsetDateTime iotCutoff = OffsetDateTime.now().minusDays(IOT_DATA_RETENTION_DAYS);
        OffsetDateTime anomalyCutoff = OffsetDateTime.now().minusYears(LONG_TERM_RETENTION_YEARS);
        LocalDate reportCutoff = LocalDate.now().minusYears(LONG_TERM_RETENTION_YEARS);

        LOG.info("Starting data cleanup: iotCutoff={}, anomalyCutoff={}, reportCutoff={}",
                iotCutoff, anomalyCutoff, reportCutoff);

        deleteIotDataInBatches(iotCutoff);
        deleteAnomalyLogs(anomalyCutoff);
        deleteReportDownloads(reportCutoff);
        deleteDailyReports(reportCutoff);

        LOG.info("Data cleanup completed");
    }

    public void deleteIotDataInBatches(OffsetDateTime cutoff) {
        int total = 0;
        int deleted;
        do {
            deleted = iotDataBatchRepository.deleteOldDataBatch(cutoff);
            total += deleted;
            if (deleted > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOG.warn("iot_data cleanup interrupted after {} rows", total);
                    return;
                }
            }
        } while (deleted > 0);
        LOG.info("iot_data cleanup: {} rows deleted", total);
    }

    public void deleteAnomalyLogs(OffsetDateTime cutoff) {
        int deleted = anomalyLogBatchRepository.deleteByDetectedAtBefore(cutoff);
        LOG.info("anomaly_logs cleanup: {} rows deleted", deleted);
    }

    public void deleteReportDownloads(LocalDate cutoff) {
        int deleted = reportDownloadCleanupRepository.deleteByDownloadDateBefore(cutoff);
        LOG.info("report_downloads cleanup: {} rows deleted", deleted);
    }

    public void deleteDailyReports(LocalDate cutoff) {
        int deleted = dailyReportRepository.deleteByReportDateBefore(cutoff);
        LOG.info("daily_reports cleanup: {} rows deleted", deleted);
    }
}
