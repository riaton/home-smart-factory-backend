package com.example.smartfactory.batch.report;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerationService.class);

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private final UserRepository userRepository;

    private final IotDataBatchRepository iotDataBatchRepository;

    private final AnomalyLogBatchRepository anomalyLogBatchRepository;

    private final DailyReportRepository dailyReportRepository;

    private final ObjectMapper objectMapper;

    public void generateDailyReports() {
        LocalDate reportDate = LocalDate.now(JST).minusDays(1);
        OffsetDateTime from = reportDate.atStartOfDay(JST).toOffsetDateTime();
        OffsetDateTime to = reportDate.plusDays(1).atStartOfDay(JST).toOffsetDateTime();

        LOG.info("Generating daily reports for date={}", reportDate);

        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                generateForUser(user.getId(), reportDate, from, to);
            } catch (Exception e) {
                LOG.error("batch report generation failed: user_id={}", user.getId(), e);
            }
        }
    }

    private void generateForUser(UUID userId, LocalDate reportDate,
            OffsetDateTime from, OffsetDateTime to) {
        if (dailyReportRepository.existsByUserIdAndReportDate(userId, reportDate)) {
            LOG.debug("Report already exists for user={}, date={}, skipping", userId, reportDate);
            return;
        }

        IotDataAggregate iotAgg = iotDataBatchRepository
                .findAggregateByUserAndDateRange(userId, from, to);

        List<AnomalyGroupResult> anomalyGroups = anomalyLogBatchRepository
                .findGroupedByUserAndDateRange(userId, from, to);

        int anomalyCount = (int) anomalyGroups.stream()
                .mapToLong(AnomalyGroupResult::getCount)
                .sum();

        String anomalySummary = buildAnomalySummary(anomalyGroups);

        Long rawMotionMinutes = iotAgg.getTotalMotionMinutes();
        Integer totalMotionMinutes = rawMotionMinutes != null ? rawMotionMinutes.intValue() : null;

        DailyReport report = DailyReport.create(
                userId, reportDate,
                iotAgg.getTotalPowerKwh(),
                iotAgg.getAvgTemperature(),
                iotAgg.getAvgHumidity(),
                totalMotionMinutes,
                anomalyCount,
                anomalySummary);

        dailyReportRepository.save(report);
        LOG.info("Report generated for user={}, date={}", userId, reportDate);
    }

    private String buildAnomalySummary(List<AnomalyGroupResult> groups) {
        if (groups.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> summary = groups.stream()
                .map(g -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("device_id", g.getDeviceId());
                    item.put("metric_type", g.getMetricType());
                    item.put("count", g.getCount());
                    item.put("max_value", g.getMaxActualValue());
                    return item;
                })
                .toList();
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize anomaly_summary", e);
        }
    }
}
