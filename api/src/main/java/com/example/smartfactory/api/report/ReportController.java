package com.example.smartfactory.api.report;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.report.dto.ReportDetailResponse;
import com.example.smartfactory.api.report.dto.ReportDownloadResult;
import com.example.smartfactory.api.report.dto.ReportListItemResponse;
import com.example.smartfactory.common.response.ApiResponse;
import com.example.smartfactory.common.response.PagedResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final int MAX_PER_PAGE = 100;

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<PagedResponse<ReportListItemResponse>> getReports(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        int effectivePerPage = Math.min(perPage, MAX_PER_PAGE);
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(reportService.getReports(userId, page, effectivePerPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReport(
            HttpSession session,
            @PathVariable UUID id) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(reportService.getReport(userId, id)));
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(
            HttpSession session,
            @PathVariable UUID id) {
        UUID userId = currentUserId(session);
        ReportDownloadResult result = reportService.downloadReport(userId, id);
        String filename = "report_" + result.reportDate() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(result.pdf());
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
