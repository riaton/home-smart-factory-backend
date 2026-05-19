package com.example.smartfactory.api.anomalylog;

import com.example.smartfactory.api.anomalylog.dto.AnomalyLogResponse;
import com.example.smartfactory.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnomalyLogService {

    private static final int MAX_PER_PAGE = 100;

    private static final Set<String> VALID_METRIC_TYPES = Set.of("temperature", "humidity", "power_w");

    private final AnomalyLogRepository anomalyLogRepository;

    public PagedResponse<AnomalyLogResponse> findAll(
            UUID userId, String deviceId, String metricType,
            OffsetDateTime from, OffsetDateTime to, int page, int perPage) {
        if (page < 1) {
            throw new IllegalArgumentException("page は1以上を指定してください");
        }
        if (metricType != null && !VALID_METRIC_TYPES.contains(metricType)) {
            throw new IllegalArgumentException("metric_type の値が不正です: " + metricType);
        }
        int clampedPerPage = Math.max(1, Math.min(perPage, MAX_PER_PAGE));
        Page<AnomalyLogResponse> result = anomalyLogRepository
                .findByFilter(userId, deviceId, metricType, from, to, PageRequest.of(page - 1, clampedPerPage))
                .map(AnomalyLogResponse::from);
        return new PagedResponse<>(
                result.getContent(),
                new PagedResponse.Pagination(result.getTotalElements(), page, result.getSize()));
    }
}
