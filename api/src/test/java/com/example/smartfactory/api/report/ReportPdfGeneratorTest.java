package com.example.smartfactory.api.report;

import com.example.smartfactory.api.report.dto.AnomalySummaryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ReportPdfGeneratorTest {

    private ReportPdfGenerator generator;

    @BeforeEach
    void setup() {
        generator = new ReportPdfGenerator();
    }

    @Test
    @DisplayName("generate は有効な PDF バイナリを返すこと")
    void generate_returnsValidPdfBytes() {
        DailyReport report = mock(DailyReport.class);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(report.getTotalPowerKwh()).willReturn(new BigDecimal("3.52"));
        given(report.getAvgTemperature()).willReturn(new BigDecimal("22.10"));
        given(report.getAvgHumidity()).willReturn(new BigDecimal("58.30"));
        given(report.getTotalMotionMinutes()).willReturn(420);
        given(report.getAnomalyCount()).willReturn(2);
        given(report.getCreatedAt()).willReturn(OffsetDateTime.parse("2026-01-15T03:00:00Z"));

        byte[] result = generator.generate(report, List.of());

        assertThat(result).isNotEmpty();
        assertThat(result[0]).isEqualTo((byte) 0x25);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x44);
        assertThat(result[3]).isEqualTo((byte) 0x46);
    }

    @Test
    @DisplayName("generate は anomaly_summary を含む PDF を返すこと")
    void generate_withAnomalySummary_returnsValidPdfBytes() {
        DailyReport report = mock(DailyReport.class);
        given(report.getReportDate()).willReturn(LocalDate.of(2026, 1, 14));
        given(report.getTotalPowerKwh()).willReturn(null);
        given(report.getAvgTemperature()).willReturn(null);
        given(report.getAvgHumidity()).willReturn(null);
        given(report.getTotalMotionMinutes()).willReturn(null);
        given(report.getAnomalyCount()).willReturn(1);
        given(report.getCreatedAt()).willReturn(OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        AnomalySummaryItem item = new AnomalySummaryItem("room01", "temperature", 1L, new BigDecimal("38.20"));

        byte[] result = generator.generate(report, List.of(item));

        assertThat(result).isNotEmpty();
        assertThat(result[0]).isEqualTo((byte) 0x25);
    }
}
