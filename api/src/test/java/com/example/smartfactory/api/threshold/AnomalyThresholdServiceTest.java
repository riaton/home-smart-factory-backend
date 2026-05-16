package com.example.smartfactory.api.threshold;

import com.example.smartfactory.api.threshold.dto.AnomalyThresholdRequest;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdResponse;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdUpdateRequest;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class AnomalyThresholdServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID THRESHOLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private AnomalyThresholdRepository thresholdRepository;

    private AnomalyThresholdService thresholdService;

    @BeforeEach
    void setup() {
        thresholdService = new AnomalyThresholdService(thresholdRepository);
    }

    @Test
    @DisplayName("getAll はユーザーの閾値設定一覧を返すこと")
    void getAll_returnsThresholdList() {
        AnomalyThreshold threshold = mock(AnomalyThreshold.class);
        given(threshold.getId()).willReturn(THRESHOLD_ID);
        given(threshold.getMetricType()).willReturn("temperature");
        given(threshold.getMinValue()).willReturn(new BigDecimal("10.00"));
        given(threshold.getMaxValue()).willReturn(new BigDecimal("35.00"));
        given(threshold.isEnabled()).willReturn(true);
        given(threshold.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(threshold.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(thresholdRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).willReturn(List.of(threshold));

        List<AnomalyThresholdResponse> result = thresholdService.getAll(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).metricType()).isEqualTo("temperature");
        assertThat(result.get(0).enabled()).isTrue();
    }

    @Test
    @DisplayName("create は新しい閾値設定を保存して返すこと")
    void create_savesAndReturnsThreshold() {
        AnomalyThresholdRequest request = new AnomalyThresholdRequest(
                "temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        AnomalyThreshold saved = mock(AnomalyThreshold.class);
        given(saved.getId()).willReturn(THRESHOLD_ID);
        given(saved.getMetricType()).willReturn("temperature");
        given(saved.getMinValue()).willReturn(new BigDecimal("10.0"));
        given(saved.getMaxValue()).willReturn(new BigDecimal("35.0"));
        given(saved.isEnabled()).willReturn(true);
        given(saved.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(saved.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(thresholdRepository.existsByUserIdAndMetricType(USER_ID, "temperature")).willReturn(false);
        given(thresholdRepository.saveAndFlush(any())).willReturn(saved);

        AnomalyThresholdResponse result = thresholdService.create(USER_ID, request);

        assertThat(result.metricType()).isEqualTo("temperature");
        assertThat(result.enabled()).isTrue();
    }

    @Test
    @DisplayName("create で power_w の場合 min_value は null で保存されること")
    void create_powerW_minValueIsNull() {
        AnomalyThresholdRequest request = new AnomalyThresholdRequest(
                "power_w", new BigDecimal("0.0"), new BigDecimal("1000.0"));
        AnomalyThreshold saved = mock(AnomalyThreshold.class);
        given(saved.getId()).willReturn(THRESHOLD_ID);
        given(saved.getMetricType()).willReturn("power_w");
        given(saved.getMinValue()).willReturn(null);
        given(saved.getMaxValue()).willReturn(new BigDecimal("1000.0"));
        given(saved.isEnabled()).willReturn(true);
        given(saved.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(saved.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(thresholdRepository.existsByUserIdAndMetricType(USER_ID, "power_w")).willReturn(false);
        given(thresholdRepository.saveAndFlush(any())).willReturn(saved);

        AnomalyThresholdResponse result = thresholdService.create(USER_ID, request);

        assertThat(result.minValue()).isNull();
        assertThat(result.maxValue()).isEqualByComparingTo("1000.0");
    }

    @Test
    @DisplayName("create で同一 metric_type が既に存在する場合 DuplicateResourceException をスローすること")
    void create_duplicateMetricType_throwsDuplicateResourceException() {
        AnomalyThresholdRequest request = new AnomalyThresholdRequest(
                "temperature", new BigDecimal("10.0"), new BigDecimal("35.0"));
        given(thresholdRepository.existsByUserIdAndMetricType(USER_ID, "temperature")).willReturn(true);

        assertThatThrownBy(() -> thresholdService.create(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class);

        then(thresholdRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("update は閾値設定を更新して返すこと")
    void update_updatesAndReturnsThreshold() {
        AnomalyThresholdUpdateRequest request = new AnomalyThresholdUpdateRequest(
                null, new BigDecimal("40.0"), false);
        AnomalyThreshold threshold = mock(AnomalyThreshold.class);
        given(threshold.getId()).willReturn(THRESHOLD_ID);
        given(threshold.getMetricType()).willReturn("temperature");
        given(threshold.getMinValue()).willReturn(new BigDecimal("10.0"));
        given(threshold.getMaxValue()).willReturn(new BigDecimal("40.0"));
        given(threshold.isEnabled()).willReturn(false);
        given(threshold.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(threshold.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 1, 2, 0, 0, 0));
        given(thresholdRepository.findByIdAndUserId(THRESHOLD_ID, USER_ID)).willReturn(Optional.of(threshold));

        AnomalyThresholdResponse result = thresholdService.update(USER_ID, THRESHOLD_ID, request);

        then(threshold).should().update(null, new BigDecimal("40.0"), false);
        assertThat(result.enabled()).isFalse();
    }

    @Test
    @DisplayName("update で閾値設定が存在しない場合 ResourceNotFoundException をスローすること")
    void update_notFound_throwsResourceNotFoundException() {
        AnomalyThresholdUpdateRequest request = new AnomalyThresholdUpdateRequest(
                null, new BigDecimal("40.0"), null);
        given(thresholdRepository.findByIdAndUserId(THRESHOLD_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> thresholdService.update(USER_ID, THRESHOLD_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete は閾値設定を削除すること")
    void delete_deletesThreshold() {
        AnomalyThreshold threshold = mock(AnomalyThreshold.class);
        given(thresholdRepository.findByIdAndUserId(THRESHOLD_ID, USER_ID)).willReturn(Optional.of(threshold));

        thresholdService.delete(USER_ID, THRESHOLD_ID);

        then(thresholdRepository).should().delete(threshold);
    }

    @Test
    @DisplayName("delete で閾値設定が存在しない場合 ResourceNotFoundException をスローすること")
    void delete_notFound_throwsResourceNotFoundException() {
        given(thresholdRepository.findByIdAndUserId(THRESHOLD_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> thresholdService.delete(USER_ID, THRESHOLD_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
