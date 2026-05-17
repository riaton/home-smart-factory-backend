package com.example.smartfactory.api.iotdata;

import com.example.smartfactory.api.iotdata.dto.IotDataResponse;
import com.example.smartfactory.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IotDataService {

    private static final int MAX_PER_PAGE = 1000;

    private final IotDataRepository iotDataRepository;

    public PagedResponse<IotDataResponse> findAll(
            UUID userId, String deviceId, OffsetDateTime from, OffsetDateTime to, int page, int perPage) {
        if (page < 1) {
            throw new IllegalArgumentException("page は1以上を指定してください");
        }
        int clampedPerPage = Math.max(1, Math.min(perPage, MAX_PER_PAGE));
        Page<IotDataResponse> result = iotDataRepository
                .findByFilter(userId, deviceId, from, to, PageRequest.of(page - 1, clampedPerPage))
                .map(IotDataResponse::from);
        return new PagedResponse<>(
                result.getContent(),
                new PagedResponse.Pagination(result.getTotalElements(), page, result.getSize()));
    }
}
