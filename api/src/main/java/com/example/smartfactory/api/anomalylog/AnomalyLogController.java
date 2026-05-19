package com.example.smartfactory.api.anomalylog;

import com.example.smartfactory.api.anomalylog.dto.AnomalyLogResponse;
import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.common.response.PagedResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/anomaly-logs")
@RequiredArgsConstructor
public class AnomalyLogController {

    private final AnomalyLogService anomalyLogService;

    @GetMapping
    public ResponseEntity<PagedResponse<AnomalyLogResponse>> getAnomalyLogs(
            HttpSession session,
            @RequestParam(name = "device_id", required = false) String deviceId,
            @RequestParam(name = "metric_type", required = false) String metricType,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "20") int perPage) {
        return ResponseEntity.ok(
                anomalyLogService.findAll(currentUserId(session), deviceId, metricType, from, to, page, perPage));
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
