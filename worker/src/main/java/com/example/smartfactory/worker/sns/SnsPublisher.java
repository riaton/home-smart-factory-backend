package com.example.smartfactory.worker.sns;

import com.example.smartfactory.worker.anomaly.AnomalyLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SnsPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(SnsPublisher.class);

    private static final int MAX_RETRIES = 3;

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private static final DateTimeFormatter JST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SnsClient snsClient;

    private final SnsProperties snsProperties;

    public void publish(AnomalyLog anomalyLog) {
        String subject = buildSubject(anomalyLog);
        String body = buildBody(anomalyLog);

        PublishRequest request = PublishRequest.builder()
                .topicArn(snsProperties.topicArn())
                .subject(subject)
                .message(body)
                .build();

        int attempt = 0;
        long delayMs = 1000;

        while (attempt < MAX_RETRIES) {
            try {
                snsClient.publish(request);
                return;
            } catch (InvalidParameterException e) {
                LOG.error("SNS Publish failed: invalid destination: {}", e.getMessage());
                return;
            } catch (SnsException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    LOG.error("SNS Publish failed: retry limit exceeded: {}", e.getMessage());
                    return;
                }
                LOG.warn("SNS Publish failed (attempt {}), retrying in {}ms: {}", attempt, delayMs, e.getMessage());
                sleep(delayMs);
                delayMs *= 2;
            }
        }
    }

    private String buildSubject(AnomalyLog log) {
        return "[異常検知] デバイス " + log.getDeviceId() + " - " + log.getMetricType() + "異常";
    }

    private String buildBody(AnomalyLog log) {
        String unit = unitOf(log.getMetricType());
        String detectedAtJst = log.getDetectedAt()
                .atZoneSameInstant(JST)
                .format(JST_FORMATTER) + " JST";

        String direction = log.getMessage().contains("上限") ? "上限" : "下限";

        return "デバイス: " + log.getDeviceId() + "\n"
                + "検知項目: " + log.getMetricType() + "\n"
                + "設定閾値: " + log.getThresholdValue() + unit + "（" + direction + "）\n"
                + "実測値: " + log.getActualValue() + unit + "\n"
                + "検知日時: " + detectedAtJst;
    }

    private static String unitOf(String metricType) {
        return switch (metricType) {
            case "temperature" -> "℃";
            case "humidity" -> "%";
            case "power_w" -> "W";
            default -> "";
        };
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
