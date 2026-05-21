package com.example.smartfactory.api.report;

import com.example.smartfactory.api.report.dto.ReportDetailResponse;
import com.example.smartfactory.api.report.dto.ReportDownloadResult;
import com.example.smartfactory.api.report.dto.ReportListItemResponse;
import com.example.smartfactory.common.exception.DownloadLimitExceededException;
import com.example.smartfactory.common.exception.ReportNotFoundException;
import com.example.smartfactory.common.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private ReportDownloadRepository reportDownloadRepository;

    @Mock
    private ReportPdfGenerator reportPdfGenerator;

    private ReportService reportService;

    @BeforeEach
    void setup() {
        reportService = new ReportService(
                dailyReportRepository, reportDownloadRepository,
                reportPdfGenerator, new ObjectMapper());
    }

    @Test
    @DisplayName("getReports はユーザーのレポート一覧をページネーションで返すこと")
    void getReports_returnsPagedList() {
        DailyReport report = mock(DailyReport.class);
        given(report.getId()).willReturn(REPORT_ID);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(report.getTotalPowerKwh()).willReturn(new BigDecimal("3.52"));
        given(report.getAvgTemperature()).willReturn(new BigDecimal("22.10"));
        given(report.getAvgHumidity()).willReturn(new BigDecimal("58.30"));
        given(report.getTotalMotionMinutes()).willReturn(420);
        given(report.getAnomalyCount()).willReturn(2);
        given(report.getCreatedAt()).willReturn(OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        given(dailyReportRepository.findByUserIdOrderByReportDateDesc(any(), any()))
                .willReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 30L));

        PagedResponse<ReportListItemResponse> result = reportService.getReports(USER_ID, 1, 20);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).reportDate()).isEqualTo(LocalDate.of(2026, 1, 14));
        assertThat(result.pagination().total()).isEqualTo(30L);
        assertThat(result.pagination().page()).isEqualTo(1);
        assertThat(result.pagination().perPage()).isEqualTo(20);
    }

    @Test
    @DisplayName("getReport はレポート詳細と当日ダウンロード回数を返すこと")
    void getReport_returnsDetailWithDownloadCount() {
        DailyReport report = mock(DailyReport.class);
        given(report.getId()).willReturn(REPORT_ID);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(report.getTotalPowerKwh()).willReturn(new BigDecimal("3.52"));
        given(report.getAvgTemperature()).willReturn(new BigDecimal("22.10"));
        given(report.getAvgHumidity()).willReturn(new BigDecimal("58.30"));
        given(report.getTotalMotionMinutes()).willReturn(420);
        given(report.getAnomalyCount()).willReturn(2);
        given(report.getAnomalySummary()).willReturn(null);
        given(report.getCreatedAt()).willReturn(OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.of(report));
        given(reportDownloadRepository.countByUserIdAndDownloadDate(any(), any()))
                .willReturn(1L);

        ReportDetailResponse result = reportService.getReport(USER_ID, REPORT_ID);

        assertThat(result.reportDate()).isEqualTo(LocalDate.of(2026, 1, 14));
        assertThat(result.downloadCountToday()).isEqualTo(1);
        assertThat(result.anomalySummary()).isNull();
    }

    @Test
    @DisplayName("getReport でレポートが存在しない場合は ResourceNotFoundException をスローすること")
    void getReport_notFound_throwsResourceNotFoundException() {
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReport(USER_ID, REPORT_ID))
                .isInstanceOf(ReportNotFoundException.class);
    }

    @Test
    @DisplayName("downloadReport はPDFを生成しダウンロード履歴を記録すること")
    void downloadReport_generatesPdfAndRecordsDownload() {
        DailyReport report = mock(DailyReport.class);
        given(report.getAnomalySummary()).willReturn(null);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.of(report));
        given(reportDownloadRepository.countByUserIdAndDownloadDate(any(), any()))
                .willReturn(2L);
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        given(reportPdfGenerator.generate(any(), any())).willReturn(pdfBytes);

        ReportDownloadResult result = reportService.downloadReport(USER_ID, REPORT_ID);

        assertThat(result.pdf()).isEqualTo(pdfBytes);
        assertThat(result.reportDate()).isEqualTo(LocalDate.of(2026, 1, 14));
        then(reportDownloadRepository).should().save(any(ReportDownload.class));
    }

    @Test
    @DisplayName("downloadReport でダウンロード回数が3回に達している場合は DownloadLimitExceededException をスローすること")
    void downloadReport_limitReached_throwsDownloadLimitExceededException() {
        DailyReport report = mock(DailyReport.class);
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.of(report));
        given(reportDownloadRepository.countByUserIdAndDownloadDate(any(), any()))
                .willReturn(3L);

        assertThatThrownBy(() -> reportService.downloadReport(USER_ID, REPORT_ID))
                .isInstanceOf(DownloadLimitExceededException.class);

        then(reportPdfGenerator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("downloadReport でレポートが存在しない場合は ResourceNotFoundException をスローすること")
    void downloadReport_notFound_throwsResourceNotFoundException() {
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.downloadReport(USER_ID, REPORT_ID))
                .isInstanceOf(ReportNotFoundException.class);
    }

    @Test
    @DisplayName("getReport で anomaly_summary が有効な JSON の場合は AnomalySummaryItem リストを返すこと")
    void getReport_withValidAnomalySummaryJson_parsesItems() {
        String json = "[{\"device_id\":\"room01\",\"metric_type\":\"temperature\",\"count\":2,\"max_value\":38.20}]";
        DailyReport report = mock(DailyReport.class);
        given(report.getId()).willReturn(REPORT_ID);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(report.getTotalPowerKwh()).willReturn(null);
        given(report.getAvgTemperature()).willReturn(null);
        given(report.getAvgHumidity()).willReturn(null);
        given(report.getTotalMotionMinutes()).willReturn(null);
        given(report.getAnomalyCount()).willReturn(2);
        given(report.getAnomalySummary()).willReturn(json);
        given(report.getCreatedAt()).willReturn(OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        given(dailyReportRepository.findByIdAndUserId(REPORT_ID, USER_ID))
                .willReturn(Optional.of(report));
        given(reportDownloadRepository.countByUserIdAndDownloadDate(any(), any()))
                .willReturn(0L);

        ReportDetailResponse result = reportService.getReport(USER_ID, REPORT_ID);

        assertThat(result.anomalySummary()).hasSize(1);
        assertThat(result.anomalySummary().get(0).deviceId()).isEqualTo("room01");
        assertThat(result.anomalySummary().get(0).metricType()).isEqualTo("temperature");
        assertThat(result.anomalySummary().get(0).count()).isEqualTo(2L);
    }
}
