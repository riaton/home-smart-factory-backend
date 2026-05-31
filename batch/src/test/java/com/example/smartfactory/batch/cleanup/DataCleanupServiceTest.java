package com.example.smartfactory.batch.cleanup;

import com.example.smartfactory.batch.report.AnomalyLogBatchRepository;
import com.example.smartfactory.batch.report.DailyReportRepository;
import com.example.smartfactory.batch.report.IotDataBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private IotDataBatchRepository iotDataBatchRepository;

    @Mock
    private AnomalyLogBatchRepository anomalyLogBatchRepository;

    @Mock
    private ReportDownloadCleanupRepository reportDownloadCleanupRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    private DataCleanupService service;

    @BeforeEach
    void setup() {
        service = new DataCleanupService(
                iotDataBatchRepository, anomalyLogBatchRepository,
                reportDownloadCleanupRepository, dailyReportRepository);
    }

    @Test
    @DisplayName("deleteIotDataInBatches は 0 件になるまでループ削除すること")
    void deleteIotDataInBatches_loopsUntilZeroRows() {
        given(iotDataBatchRepository.deleteOldDataBatch(any(OffsetDateTime.class)))
                .willReturn(10000, 10000, 0);

        service.deleteIotDataInBatches(OffsetDateTime.now().minusDays(90));

        then(iotDataBatchRepository).should(times(3)).deleteOldDataBatch(any());
    }

    @Test
    @DisplayName("deleteIotDataInBatches は最初から 0 件の場合は 1 回だけ実行すること")
    void deleteIotDataInBatches_zeroRowsOnFirstCall_executesOnce() {
        given(iotDataBatchRepository.deleteOldDataBatch(any(OffsetDateTime.class)))
                .willReturn(0);

        service.deleteIotDataInBatches(OffsetDateTime.now().minusDays(90));

        then(iotDataBatchRepository).should(times(1)).deleteOldDataBatch(any());
    }

    @Test
    @DisplayName("deleteAnomalyLogs は anomaly_logs の削除クエリを実行すること")
    void deleteAnomalyLogs_executesDelete() {
        given(anomalyLogBatchRepository.deleteByDetectedAtBefore(any())).willReturn(5);

        service.deleteAnomalyLogs(OffsetDateTime.now().minusYears(1));

        then(anomalyLogBatchRepository).should().deleteByDetectedAtBefore(any());
    }

    @Test
    @DisplayName("deleteReportDownloads は report_downloads の削除クエリを実行すること")
    void deleteReportDownloads_executesDelete() {
        given(reportDownloadCleanupRepository.deleteByDownloadDateBefore(any())).willReturn(3);

        service.deleteReportDownloads(LocalDate.now().minusYears(1));

        then(reportDownloadCleanupRepository).should().deleteByDownloadDateBefore(any());
    }

    @Test
    @DisplayName("deleteDailyReports は daily_reports の削除クエリを実行すること")
    void deleteDailyReports_executesDelete() {
        given(dailyReportRepository.deleteByReportDateBefore(any())).willReturn(2);

        service.deleteDailyReports(LocalDate.now().minusYears(1));

        then(dailyReportRepository).should().deleteByReportDateBefore(any());
    }

    @Test
    @DisplayName("cleanupOldData は4テーブルを正しい順序で削除すること")
    void cleanupOldData_deletesAllTablesInOrder() {
        given(iotDataBatchRepository.deleteOldDataBatch(any())).willReturn(0);
        given(anomalyLogBatchRepository.deleteByDetectedAtBefore(any())).willReturn(0);
        given(reportDownloadCleanupRepository.deleteByDownloadDateBefore(any())).willReturn(0);
        given(dailyReportRepository.deleteByReportDateBefore(any())).willReturn(0);

        service.cleanupOldData();

        then(iotDataBatchRepository).should().deleteOldDataBatch(any());
        then(anomalyLogBatchRepository).should().deleteByDetectedAtBefore(any());
        then(reportDownloadCleanupRepository).should().deleteByDownloadDateBefore(any());
        then(dailyReportRepository).should().deleteByReportDateBefore(any());
    }
}
