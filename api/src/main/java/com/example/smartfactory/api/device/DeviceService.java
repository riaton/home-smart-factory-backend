package com.example.smartfactory.api.device;

import com.example.smartfactory.api.device.dto.DeviceRequest;
import com.example.smartfactory.api.device.dto.DeviceResponse;
import com.example.smartfactory.api.device.dto.DeviceUpdateRequest;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<DeviceResponse> getDevices(UUID userId) {
        return deviceRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(DeviceResponse::from)
                .toList();
    }

    @Transactional
    public DeviceResponse createDevice(UUID userId, DeviceRequest request) {
        try {
            Device device = Device.create(userId, request.deviceId(), request.name());
            return DeviceResponse.from(deviceRepository.saveAndFlush(device));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("device_id が既に登録済みです");
        }
    }

    @Transactional
    public DeviceResponse updateDevice(UUID userId, UUID deviceId, DeviceUpdateRequest request) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("デバイスが見つかりません"));
        device.updateName(request.name());
        return DeviceResponse.from(device);
    }

    @Transactional
    public void deleteDevice(UUID userId, UUID deviceId) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("デバイスが見つかりません"));
        deviceRepository.deleteAnomalyLogsByDeviceId(device.getDeviceId());
        deviceRepository.deleteIotDataByDeviceId(device.getDeviceId());
        deviceRepository.deleteById(deviceId);
    }
}
