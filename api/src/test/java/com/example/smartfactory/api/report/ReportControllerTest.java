package com.example.smartfactory.api.report;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.config.GlobalExceptionHandler;
import com.example.smartfactory.api.report.dto.AnomalySummaryItem;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    private static final String REPORT_ID = "00000000-0000-0000-0000-000000000002";

    @Mock
    private ReportService reportService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    @Test
    @DisplayName("GET /api/reports は 200 とページネーション付きレポート一覧を返すこと")
    void getReports_returns200WithPagedList() throws Exception {
        ReportListItemResponse item = new ReportListItemResponse(
                UUID.fromString(REPORT_ID),
                LocalDate.of(2026, 1, 14),
                new BigDecimal("3.52"),
                new BigDecimal("22.10"),
                new BigDecimal("58.30"),
                420, 2,
                OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        PagedResponse<ReportListItemResponse> response =
                PagedResponse.of(List.of(item), 30L, 1, 20);
        given(reportService.getReports(eq(UUID.fromString(USER_ID)), eq(1), eq(20)))
                .willReturn(response);

        mvc.perform(get("/api/reports").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(REPORT_ID))
                .andExpect(jsonPath("$.data[0].report_date").value("2026-01-14"))
                .andExpect(jsonPath("$.data[0].anomaly_count").value(2))
                .andExpect(jsonPath("$.pagination.total").value(30))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.per_page").value(20));
    }

    @Test
    @DisplayName("GET /api/reports/{id} は 200 とレポート詳細を返すこと")
    void getReport_returns200WithDetail() throws Exception {
        AnomalySummaryItem summaryItem = new AnomalySummaryItem(
                "room01", "temperature", 2L, new BigDecimal("38.20"));
        ReportDetailResponse detail = new ReportDetailResponse(
                UUID.fromString(REPORT_ID),
                LocalDate.of(2026, 1, 14),
                new BigDecimal("3.52"),
                new BigDecimal("22.10"),
                new BigDecimal("58.30"),
                420, 2, List.of(summaryItem), 1,
                OffsetDateTime.parse("2026-01-15T03:00:00Z"));
        given(reportService.getReport(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(REPORT_ID))))
                .willReturn(detail);

        mvc.perform(get("/api/reports/" + REPORT_ID).session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(REPORT_ID))
                .andExpect(jsonPath("$.data.report_date").value("2026-01-14"))
                .andExpect(jsonPath("$.data.download_count_today").value(1))
                .andExpect(jsonPath("$.data.anomaly_summary[0].device_id").value("room01"));
    }

    @Test
    @DisplayName("GET /api/reports/{id} でレポートが存在しない場合は 404 を返すこと")
    void getReport_notFound_returns404() throws Exception {
        given(reportService.getReport(any(), any()))
                .willThrow(new ReportNotFoundException());

        mvc.perform(get("/api/reports/" + REPORT_ID).session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/reports/{id}/download は 200 と PDF バイナリを返すこと")
    void downloadReport_returns200WithPdf() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        ReportDownloadResult result = new ReportDownloadResult(pdfBytes, LocalDate.of(2026, 1, 14));
        given(reportService.downloadReport(eq(UUID.fromString(USER_ID)), eq(UUID.fromString(REPORT_ID))))
                .willReturn(result);

        mvc.perform(post("/api/reports/" + REPORT_ID + "/download").session(session()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"report_2026-01-14.pdf\""));
    }

    @Test
    @DisplayName("POST /api/reports/{id}/download でダウンロード上限超過の場合は 429 を返すこと")
    void downloadReport_limitExceeded_returns429() throws Exception {
        given(reportService.downloadReport(any(), any()))
                .willThrow(new DownloadLimitExceededException());

        mvc.perform(post("/api/reports/" + REPORT_ID + "/download").session(session()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("DOWNLOAD_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("POST /api/reports/{id}/download でレポートが存在しない場合は 404 を返すこと")
    void downloadReport_notFound_returns404() throws Exception {
        given(reportService.downloadReport(any(), any()))
                .willThrow(new ReportNotFoundException());

        mvc.perform(post("/api/reports/" + REPORT_ID + "/download").session(session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
    }
}
