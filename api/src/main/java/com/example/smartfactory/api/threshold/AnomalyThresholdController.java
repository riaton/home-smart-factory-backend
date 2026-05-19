package com.example.smartfactory.api.threshold;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdRequest;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdResponse;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdUpdateRequest;
import com.example.smartfactory.common.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/anomaly-thresholds")
@RequiredArgsConstructor
public class AnomalyThresholdController {

    private final AnomalyThresholdService thresholdService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnomalyThresholdResponse>>> getAll(HttpSession session) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(thresholdService.getAll(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnomalyThresholdResponse>> create(
            HttpSession session,
            @Valid @RequestBody AnomalyThresholdRequest request) {
        UUID userId = currentUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(thresholdService.create(userId, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AnomalyThresholdResponse>> update(
            HttpSession session,
            @PathVariable UUID id,
            @Valid @RequestBody AnomalyThresholdUpdateRequest request) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(thresholdService.update(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpSession session, @PathVariable UUID id) {
        UUID userId = currentUserId(session);
        thresholdService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
