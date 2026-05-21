package com.example.smartfactory.api.report;

import com.example.smartfactory.api.report.dto.AnomalySummaryItem;
import com.example.smartfactory.api.report.dto.ReportDetailResponse;
import com.example.smartfactory.api.report.dto.ReportDownloadResult;
import com.example.smartfactory.api.report.dto.ReportListItemResponse;
import com.example.smartfactory.common.exception.DownloadLimitExceededException;
import com.example.smartfactory.common.exception.ReportNotFoundException;
import com.example.smartfactory.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportService.class);

    private static final int MAX_DOWNLOAD_PER_DAY = 3;

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private final DailyReportRepository dailyReportRepository;

    private final ReportDownloadRepository reportDownloadRepository;

    private final ReportPdfGenerator reportPdfGenerator;

    private final ObjectMapper objectMapper;

    public PagedResponse<ReportListItemResponse> getReports(UUID userId, int page, int perPage) {
        Page<DailyReport> reports = dailyReportRepository
                .findByUserIdOrderByReportDateDesc(userId, PageRequest.of(page - 1, perPage));
        List<ReportListItemResponse> items = reports.getContent().stream()
                .map(ReportListItemResponse::from)
                .toList();
        return PagedResponse.of(items, reports.getTotalElements(), page, perPage);
    }

    public ReportDetailResponse getReport(UUID userId, UUID reportId) {
        DailyReport report = dailyReportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(ReportNotFoundException::new);
        LocalDate today = LocalDate.now(JST);
        long count = reportDownloadRepository.countByUserIdAndDownloadDate(userId, today);
        List<AnomalySummaryItem> anomalySummary = parseAnomalySummary(report.getAnomalySummary());
        return ReportDetailResponse.from(report, anomalySummary, (int) count);
    }

    @Transactional
    public ReportDownloadResult downloadReport(UUID userId, UUID reportId) {
        DailyReport report = dailyReportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(ReportNotFoundException::new);
        LocalDate today = LocalDate.now(JST);
        long count = reportDownloadRepository.countByUserIdAndDownloadDate(userId, today);
        if (count >= MAX_DOWNLOAD_PER_DAY) {
            throw new DownloadLimitExceededException();
        }
        List<AnomalySummaryItem> anomalySummary = parseAnomalySummary(report.getAnomalySummary());
        byte[] pdf = reportPdfGenerator.generate(report, anomalySummary);
        reportDownloadRepository.save(ReportDownload.create(userId, reportId, today));
        return new ReportDownloadResult(pdf, report.getReportDate());
    }

    private List<AnomalySummaryItem> parseAnomalySummary(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AnomalySummaryItem.class));
        } catch (Exception e) {
            LOG.warn("anomaly_summary の JSON 解析に失敗しました: {}", e.getMessage());
            return null;
        }
    }
}
