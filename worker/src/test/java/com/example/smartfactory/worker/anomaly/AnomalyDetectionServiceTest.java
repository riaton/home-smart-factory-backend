package com.example.smartfactory.worker.anomaly;

import com.example.smartfactory.worker.iotdata.IotMessagePayload;
import com.example.smartfactory.worker.sns.SnsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String DEVICE_ID = "room01";

    @Mock
    private AnomalyThresholdRepository thresholdRepository;

    @Mock
    private AnomalyLogRepository anomalyLogRepository;

    @Mock
    private SnsPublisher snsPublisher;

    private AnomalyDetectionService service;

    @BeforeEach
    void setup() {
        service = new AnomalyDetectionService(thresholdRepository, anomalyLogRepository, snsPublisher);
    }

    private IotMessagePayload payload(BigDecimal temperature, BigDecimal humidity, BigDecimal powerW) {
        return new IotMessagePayload(DEVICE_ID, temperature, humidity, null, powerW,
                OffsetDateTime.parse("2026-01-15T10:00:00+09:00"));
    }

    private AnomalyThreshold threshold(String metricType, BigDecimal min, BigDecimal max) {
        AnomalyThreshold t = mock(AnomalyThreshold.class);
        lenient().when(t.getMetricType()).thenReturn(metricType);
        lenient().when(t.getMinValue()).thenReturn(min);
        lenient().when(t.getMaxValue()).thenReturn(max);
        return t;
    }

    @Test
    @DisplayName("有効な閾値が存在しない場合は何もしないこと")
    void detect_noThresholds_doesNothing() {
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of());

        service.detect(USER_ID, DEVICE_ID, payload(new BigDecimal("40.0"), null, null));

        then(anomalyLogRepository).shouldHaveNoInteractions();
        then(snsPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("温度が上限閾値を超えた場合 anomaly_log を保存して SNS Publish すること")
    void detect_temperatureExceedsMax_savesLogAndPublishes() {
        AnomalyThreshold t = threshold("temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.detect(USER_ID, DEVICE_ID, payload(new BigDecimal("38.2"), null, null));

        ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
        then(anomalyLogRepository).should().save(captor.capture());
        AnomalyLog log = captor.getValue();
        assertThat(log.getMetricType()).isEqualTo("temperature");
        assertThat(log.getThresholdValue()).isEqualByComparingTo("35.0");
        assertThat(log.getActualValue()).isEqualByComparingTo("38.2");
        assertThat(log.getMessage()).contains("上限閾値").contains("35.0℃").contains("38.2℃");
        then(snsPublisher).should().publish(any());
    }

    @Test
    @DisplayName("温度が下限閾値を下回った場合 anomaly_log を保存して SNS Publish すること")
    void detect_temperatureBelowMin_savesLogAndPublishes() {
        AnomalyThreshold t = threshold("temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.detect(USER_ID, DEVICE_ID, payload(new BigDecimal("7.5"), null, null));

        ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
        then(anomalyLogRepository).should().save(captor.capture());
        AnomalyLog log = captor.getValue();
        assertThat(log.getThresholdValue()).isEqualByComparingTo("10.0");
        assertThat(log.getActualValue()).isEqualByComparingTo("7.5");
        assertThat(log.getMessage()).contains("下限閾値").contains("10.0℃").contains("7.5℃");
        then(snsPublisher).should().publish(any());
    }

    @Test
    @DisplayName("power_w が上限を超えた場合 anomaly_log を保存すること")
    void detect_powerWExceedsMax_savesLog() {
        AnomalyThreshold t = threshold("power_w", null, new BigDecimal("500.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.detect(USER_ID, DEVICE_ID, payload(null, null, new BigDecimal("620.3")));

        ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
        then(anomalyLogRepository).should().save(captor.capture());
        AnomalyLog log = captor.getValue();
        assertThat(log.getMetricType()).isEqualTo("power_w");
        assertThat(log.getMessage()).contains("上限閾値").contains("500.0W").contains("620.3W");
    }

    @Test
    @DisplayName("温度が閾値内の場合は何もしないこと")
    void detect_temperatureWithinRange_doesNothing() {
        AnomalyThreshold t = threshold("temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));

        service.detect(USER_ID, DEVICE_ID, payload(new BigDecimal("25.0"), null, null));

        then(anomalyLogRepository).should(never()).save(any());
        then(snsPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("actual_value が null の場合はスキップすること")
    void detect_nullActualValue_skips() {
        AnomalyThreshold t = threshold("temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));

        service.detect(USER_ID, DEVICE_ID, payload(null, null, null));

        then(anomalyLogRepository).should(never()).save(any());
        then(snsPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("anomaly_logs INSERT 失敗時は SNS Publish しないこと")
    void detect_saveThrows_doesNotPublish() {
        AnomalyThreshold t = threshold("temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willThrow(new RuntimeException("DB error"));

        try {
            service.detect(USER_ID, DEVICE_ID, payload(new BigDecimal("40.0"), null, null));
        } catch (Exception ignored) {
            // IotMessageProcessor がキャッチする
        }

        then(snsPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("power_w は min_value を無視して上限のみ判定すること")
    void detect_powerW_ignoresMinValue() {
        AnomalyThreshold t = threshold("power_w", new BigDecimal("0.0"), new BigDecimal("500.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));

        service.detect(USER_ID, DEVICE_ID, payload(null, null, new BigDecimal("300.0")));

        then(anomalyLogRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("湿度が上限閾値を超えた場合 anomaly_log を保存して SNS Publish すること")
    void detect_humidityExceedsMax_savesLogAndPublishes() {
        AnomalyThreshold t = threshold("humidity", new BigDecimal("30.0"), new BigDecimal("80.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.detect(USER_ID, DEVICE_ID, payload(null, new BigDecimal("85.0"), null));

        ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
        then(anomalyLogRepository).should().save(captor.capture());
        AnomalyLog log = captor.getValue();
        assertThat(log.getMetricType()).isEqualTo("humidity");
        assertThat(log.getMessage()).contains("上限閾値").contains("80.0%").contains("85.0%");
        then(snsPublisher).should().publish(any());
    }

    @Test
    @DisplayName("湿度が下限閾値を下回った場合 anomaly_log を保存して SNS Publish すること")
    void detect_humidityBelowMin_savesLogAndPublishes() {
        AnomalyThreshold t = threshold("humidity", new BigDecimal("30.0"), new BigDecimal("80.0"));
        given(thresholdRepository.findByUserIdAndEnabledTrue(USER_ID)).willReturn(List.of(t));
        given(anomalyLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.detect(USER_ID, DEVICE_ID, payload(null, new BigDecimal("20.0"), null));

        ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
        then(anomalyLogRepository).should().save(captor.capture());
        AnomalyLog log = captor.getValue();
        assertThat(log.getMessage()).contains("下限閾値").contains("30.0%").contains("20.0%");
        then(snsPublisher).should().publish(any());
    }
}
