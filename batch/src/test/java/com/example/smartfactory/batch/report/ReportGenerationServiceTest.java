package com.example.smartfactory.batch.report;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private UserRepository userRepository;

    @Mock
    private IotDataBatchRepository iotDataBatchRepository;

    @Mock
    private AnomalyLogBatchRepository anomalyLogBatchRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    private ReportGenerationService service;

    @BeforeEach
    void setup() {
        service = new ReportGenerationService(
                userRepository, iotDataBatchRepository,
                anomalyLogBatchRepository, dailyReportRepository,
                new ObjectMapper());
    }

    private User mockUser(UUID userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        return user;
    }

    private IotDataAggregate mockIotAgg(BigDecimal avgTemp, BigDecimal avgHumidity,
            BigDecimal totalPowerKwh, Long motionMinutes) {
        IotDataAggregate agg = mock(IotDataAggregate.class);
        given(agg.getAvgTemperature()).willReturn(avgTemp);
        given(agg.getAvgHumidity()).willReturn(avgHumidity);
        given(agg.getTotalPowerKwh()).willReturn(totalPowerKwh);
        given(agg.getTotalMotionMinutes()).willReturn(motionMinutes);
        return agg;
    }

    @Test
    @DisplayName("generateDailyReports は全ユーザーのレポートを生成すること")
    void generateDailyReports_withUsers_generatesReports() {
        User user = mockUser(USER_ID);
        given(userRepository.findAll()).willReturn(List.of(user));
        given(dailyReportRepository.existsByUserIdAndReportDate(eq(USER_ID), any()))
                .willReturn(false);
        IotDataAggregate iotAgg = mockIotAgg(
                new BigDecimal("25.00"), new BigDecimal("60.00"),
                new BigDecimal("1.2000"), 480L);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(iotAgg);
        given(anomalyLogBatchRepository.findGroupedByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of());

        service.generateDailyReports();

        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        then(dailyReportRepository).should().save(captor.capture());
        DailyReport saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getAvgTemperature()).isEqualByComparingTo("25.00");
        assertThat(saved.getTotalMotionMinutes()).isEqualTo(480);
        assertThat(saved.getAnomalyCount()).isZero();
        assertThat(saved.getAnomalySummary()).isNull();
    }

    @Test
    @DisplayName("レポートが既に存在する場合は INSERT をスキップすること")
    void generateDailyReports_reportAlreadyExists_skipsInsert() {
        User user = mockUser(USER_ID);
        given(userRepository.findAll()).willReturn(List.of(user));
        given(dailyReportRepository.existsByUserIdAndReportDate(eq(USER_ID), any()))
                .willReturn(true);

        service.generateDailyReports();

        then(dailyReportRepository).should(never()).save(any());
        then(iotDataBatchRepository).should(never()).findAggregateByUserAndDateRange(any(), any(), any());
    }

    @Test
    @DisplayName("iot_data が0件の場合は NULL 値でレポートを生成すること")
    void generateDailyReports_iotDataEmpty_generatesNullReport() {
        User user = mockUser(USER_ID);
        given(userRepository.findAll()).willReturn(List.of(user));
        given(dailyReportRepository.existsByUserIdAndReportDate(eq(USER_ID), any()))
                .willReturn(false);
        IotDataAggregate iotAgg = mockIotAgg(null, null, null, null);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(iotAgg);
        given(anomalyLogBatchRepository.findGroupedByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of());

        service.generateDailyReports();

        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        then(dailyReportRepository).should().save(captor.capture());
        DailyReport saved = captor.getValue();
        assertThat(saved.getAvgTemperature()).isNull();
        assertThat(saved.getAvgHumidity()).isNull();
        assertThat(saved.getTotalPowerKwh()).isNull();
        assertThat(saved.getTotalMotionMinutes()).isNull();
        assertThat(saved.getAnomalyCount()).isZero();
    }

    @Test
    @DisplayName("異常データがある場合は anomaly_summary JSON を構築すること")
    void generateDailyReports_withAnomalies_buildsAnomalySummaryJson() {
        User user = mockUser(USER_ID);
        given(userRepository.findAll()).willReturn(List.of(user));
        given(dailyReportRepository.existsByUserIdAndReportDate(eq(USER_ID), any()))
                .willReturn(false);
        IotDataAggregate iotAgg = mockIotAgg(null, null, null, null);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(iotAgg);

        AnomalyGroupResult group = mock(AnomalyGroupResult.class);
        given(group.getDeviceId()).willReturn("room01");
        given(group.getMetricType()).willReturn("temperature");
        given(group.getCount()).willReturn(3L);
        given(group.getMaxActualValue()).willReturn(new BigDecimal("38.20"));
        given(anomalyLogBatchRepository.findGroupedByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of(group));

        service.generateDailyReports();

        ArgumentCaptor<DailyReport> captor = ArgumentCaptor.forClass(DailyReport.class);
        then(dailyReportRepository).should().save(captor.capture());
        DailyReport saved = captor.getValue();
        assertThat(saved.getAnomalyCount()).isEqualTo(3);
        assertThat(saved.getAnomalySummary()).contains("room01");
        assertThat(saved.getAnomalySummary()).contains("temperature");
        assertThat(saved.getAnomalySummary()).contains("38.20");
    }

    @Test
    @DisplayName("個別ユーザーで例外が発生した場合は次のユーザーへ続行すること")
    void generateDailyReports_userProcessingFails_continuesNextUser() {
        UUID userId1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        User user1 = mockUser(userId1);
        User user2 = mockUser(userId2);
        given(userRepository.findAll()).willReturn(List.of(user1, user2));
        given(dailyReportRepository.existsByUserIdAndReportDate(any(), any())).willReturn(false);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(userId1), any(), any()))
                .willThrow(new RuntimeException("DB error"));
        IotDataAggregate iotAgg = mockIotAgg(null, null, null, null);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(userId2), any(), any()))
                .willReturn(iotAgg);
        given(anomalyLogBatchRepository.findGroupedByUserAndDateRange(eq(userId2), any(), any()))
                .willReturn(List.of());

        service.generateDailyReports();

        then(dailyReportRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("generateDailyReports は前日の JST 日付を report_date に使用すること")
    void generateDailyReports_usesYesterdayJstAsReportDate() {
        User user = mockUser(USER_ID);
        given(userRepository.findAll()).willReturn(List.of(user));
        given(dailyReportRepository.existsByUserIdAndReportDate(eq(USER_ID), any()))
                .willReturn(false);
        IotDataAggregate iotAggJst = mockIotAgg(null, null, null, null);
        given(iotDataBatchRepository.findAggregateByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(iotAggJst);
        given(anomalyLogBatchRepository.findGroupedByUserAndDateRange(eq(USER_ID), any(), any()))
                .willReturn(List.of());

        service.generateDailyReports();

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        then(iotDataBatchRepository).should().findAggregateByUserAndDateRange(
                any(), fromCaptor.capture(), toCaptor.capture());
        then(dailyReportRepository).should().existsByUserIdAndReportDate(
                any(), dateCaptor.capture());

        LocalDate reportDate = dateCaptor.getValue();
        OffsetDateTime from = fromCaptor.getValue();
        OffsetDateTime to = toCaptor.getValue();

        assertThat(reportDate).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(1));
        assertThat(from.toLocalDate()).isEqualTo(reportDate);
        assertThat(to).isAfter(from);
    }
}
