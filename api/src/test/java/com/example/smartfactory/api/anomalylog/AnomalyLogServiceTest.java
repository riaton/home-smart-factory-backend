package com.example.smartfactory.api.anomalylog;

import com.example.smartfactory.api.anomalylog.dto.AnomalyLogResponse;
import com.example.smartfactory.common.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AnomalyLogServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private AnomalyLogRepository anomalyLogRepository;

    private AnomalyLogService anomalyLogService;

    @BeforeEach
    void setup() {
        anomalyLogService = new AnomalyLogService(anomalyLogRepository);
    }

    private AnomalyLog mockAnomalyLog() {
        AnomalyLog a = mock(AnomalyLog.class);
        given(a.getId()).willReturn(1L);
        given(a.getDeviceId()).willReturn("room01");
        given(a.getMetricType()).willReturn("temperature");
        given(a.getThresholdValue()).willReturn(new BigDecimal("35.00"));
        given(a.getActualValue()).willReturn(new BigDecimal("38.20"));
        given(a.getMessage()).willReturn("温度が上限閾値(35.0℃)を超えました: 38.2℃");
        given(a.getDetectedAt()).willReturn(OffsetDateTime.parse("2026-01-15T10:00:00Z"));
        return a;
    }

    @Test
    @DisplayName("findAll はリポジトリの結果を PagedResponse に変換して返すこと")
    void findAll_returnsPagedResponse() {
        Page<AnomalyLog> page = new PageImpl<>(List.of(mockAnomalyLog()), PageRequest.of(0, 20), 1);
        given(anomalyLogRepository.findByFilter(eq(USER_ID), isNull(), isNull(), isNull(), isNull(), any()))
                .willReturn(page);

        PagedResponse<AnomalyLogResponse> result = anomalyLogService.findAll(USER_ID, null, null, null, null, 1, 20);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).deviceId()).isEqualTo("room01");
        assertThat(result.data().get(0).metricType()).isEqualTo("temperature");
        assertThat(result.data().get(0).thresholdValue()).isEqualByComparingTo("35.00");
        assertThat(result.pagination().total()).isEqualTo(1);
        assertThat(result.pagination().page()).isEqualTo(1);
        assertThat(result.pagination().perPage()).isEqualTo(20);
    }

    @Test
    @DisplayName("page パラメータは 0-indexed に変換して Pageable に渡されること")
    void findAll_convertsPageToZeroIndexed() {
        Page<AnomalyLog> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 20), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(anomalyLogRepository.findByFilter(eq(USER_ID), isNull(), isNull(), isNull(), isNull(),
                pageableCaptor.capture())).willReturn(emptyPage);

        anomalyLogService.findAll(USER_ID, null, null, null, null, 2, 20);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("per_page が 100 を超えた場合は 100 にクランプされること")
    void findAll_clampsPerPageToMax() {
        Page<AnomalyLog> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(anomalyLogRepository.findByFilter(eq(USER_ID), isNull(), isNull(), isNull(), isNull(),
                pageableCaptor.capture())).willReturn(emptyPage);

        anomalyLogService.findAll(USER_ID, null, null, null, null, 1, 500);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("page が 0 以下の場合 IllegalArgumentException をスローすること")
    void findAll_pageZero_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> anomalyLogService.findAll(USER_ID, null, null, null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    @DisplayName("metric_type が不正値の場合 IllegalArgumentException をスローすること")
    void findAll_invalidMetricType_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> anomalyLogService.findAll(USER_ID, null, "invalid", null, null, 1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metric_type");
    }

    @Test
    @DisplayName("metric_type フィルタが指定された場合はリポジトリに渡されること")
    void findAll_passesMetricTypeFilter() {
        Page<AnomalyLog> page = new PageImpl<>(List.of(mockAnomalyLog()), PageRequest.of(0, 20), 1);
        given(anomalyLogRepository.findByFilter(eq(USER_ID), isNull(), eq("temperature"), isNull(), isNull(), any()))
                .willReturn(page);

        PagedResponse<AnomalyLogResponse> result =
                anomalyLogService.findAll(USER_ID, null, "temperature", null, null, 1, 20);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).metricType()).isEqualTo("temperature");
    }

    @Test
    @DisplayName("from/to フィルタが指定された場合はリポジトリに渡されること")
    void findAll_passesDateRangeFilter() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");
        Page<AnomalyLog> page = new PageImpl<>(List.of(mockAnomalyLog()), PageRequest.of(0, 20), 1);
        given(anomalyLogRepository.findByFilter(eq(USER_ID), isNull(), isNull(), eq(from), eq(to), any()))
                .willReturn(page);

        PagedResponse<AnomalyLogResponse> result =
                anomalyLogService.findAll(USER_ID, null, null, from, to, 1, 20);

        assertThat(result.data()).hasSize(1);
    }
}
