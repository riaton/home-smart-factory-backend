package com.example.smartfactory.worker.anomaly;

import com.example.smartfactory.worker.iotdata.IotMessagePayload;
import com.example.smartfactory.worker.sns.SnsPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnomalyDetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AnomalyThresholdRepository thresholdRepository;

    private final AnomalyLogRepository anomalyLogRepository;

    private final SnsPublisher snsPublisher;

    @Transactional
    public void detect(UUID userId, String deviceId, IotMessagePayload payload) {
        List<AnomalyThreshold> thresholds = thresholdRepository.findByUserIdAndEnabledTrue(userId);
        if (thresholds.isEmpty()) {
            LOG.debug("No enabled thresholds for user {}, skipping anomaly detection", userId);
            return;
        }

        List<AnomalyLog> savedLogs = new ArrayList<>();
        for (AnomalyThreshold threshold : thresholds) {
            BigDecimal actualValue = actualValueOf(threshold.getMetricType(), payload);
            if (actualValue == null) {
                continue;
            }

            AnomalyLog anomalyLog = checkThreshold(userId, deviceId, threshold, actualValue);
            if (anomalyLog != null) {
                savedLogs.add(anomalyLogRepository.save(anomalyLog));
            }
        }

        // anomaly_logs INSERT 成功後に SNS Publish する。
        // トランザクションがアクティブな場合はコミット後に実行し、INSERT失敗時の通知送信を防ぐ。
        if (!savedLogs.isEmpty()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (AnomalyLog log : savedLogs) {
                            snsPublisher.publish(log);
                        }
                    }
                });
            } else {
                for (AnomalyLog log : savedLogs) {
                    snsPublisher.publish(log);
                }
            }
        }
    }

    private AnomalyLog checkThreshold(UUID userId, String deviceId,
            AnomalyThreshold threshold, BigDecimal actualValue) {
        String metricType = threshold.getMetricType();
        BigDecimal minValue = threshold.getMinValue();
        BigDecimal maxValue = threshold.getMaxValue();

        if ("power_w".equals(metricType)) {
            if (maxValue != null && actualValue.compareTo(maxValue) > 0) {
                String message = buildMessage(metricType, maxValue, actualValue, true);
                return AnomalyLog.create(userId, deviceId, metricType, maxValue, actualValue, message);
            }
            return null;
        }

        if (minValue != null && actualValue.compareTo(minValue) < 0) {
            String message = buildMessage(metricType, minValue, actualValue, false);
            return AnomalyLog.create(userId, deviceId, metricType, minValue, actualValue, message);
        }
        if (maxValue != null && actualValue.compareTo(maxValue) > 0) {
            String message = buildMessage(metricType, maxValue, actualValue, true);
            return AnomalyLog.create(userId, deviceId, metricType, maxValue, actualValue, message);
        }
        return null;
    }

    private static BigDecimal actualValueOf(String metricType, IotMessagePayload payload) {
        return switch (metricType) {
            case "temperature" -> payload.temperature();
            case "humidity" -> payload.humidity();
            case "power_w" -> payload.powerW();
            default -> null;
        };
    }

    private static String buildMessage(String metricType, BigDecimal thresholdValue,
            BigDecimal actualValue, boolean isUpperBreach) {
        String name = metricTypeName(metricType);
        String unit = unitOf(metricType);
        if (isUpperBreach) {
            return name + "が上限閾値(" + thresholdValue + unit + ")を超えました: " + actualValue + unit;
        }
        return name + "が下限閾値(" + thresholdValue + unit + ")を下回りました: " + actualValue + unit;
    }

    private static String metricTypeName(String metricType) {
        return switch (metricType) {
            case "temperature" -> "温度";
            case "humidity" -> "湿度";
            case "power_w" -> "消費電力";
            default -> metricType;
        };
    }

    private static String unitOf(String metricType) {
        return switch (metricType) {
            case "temperature" -> "℃";
            case "humidity" -> "%";
            case "power_w" -> "W";
            default -> "";
        };
    }
}
