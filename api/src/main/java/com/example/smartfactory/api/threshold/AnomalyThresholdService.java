package com.example.smartfactory.api.threshold;

import com.example.smartfactory.api.threshold.dto.AnomalyThresholdRequest;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdResponse;
import com.example.smartfactory.api.threshold.dto.AnomalyThresholdUpdateRequest;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnomalyThresholdService {

    private final AnomalyThresholdRepository thresholdRepository;

    public List<AnomalyThresholdResponse> getAll(UUID userId) {
        return thresholdRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(AnomalyThresholdResponse::from)
                .toList();
    }

    @Transactional
    public AnomalyThresholdResponse create(UUID userId, AnomalyThresholdRequest request) {
        if (thresholdRepository.existsByUserIdAndMetricType(userId, request.metricType())) {
            throw new DuplicateResourceException("同一の metric_type の閾値設定が既に存在します");
        }
        try {
            BigDecimal minValue = "power_w".equals(request.metricType()) ? null : request.minValue();
            AnomalyThreshold threshold = AnomalyThreshold.create(
                    userId, request.metricType(), minValue, request.maxValue());
            return AnomalyThresholdResponse.from(thresholdRepository.saveAndFlush(threshold));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("同一の metric_type の閾値設定が既に存在します");
        }
    }

    @Transactional
    public AnomalyThresholdResponse update(UUID userId, UUID id, AnomalyThresholdUpdateRequest request) {
        AnomalyThreshold threshold = thresholdRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("閾値設定が見つかりません"));
        threshold.update(request.minValue(), request.maxValue(), request.enabled());
        return AnomalyThresholdResponse.from(threshold);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        AnomalyThreshold threshold = thresholdRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("閾値設定が見つかりません"));
        thresholdRepository.delete(threshold);
    }
}
