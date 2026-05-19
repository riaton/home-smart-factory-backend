package com.example.smartfactory.worker.iotdata;

import com.example.smartfactory.common.exception.ResourceNotFoundException;
import com.example.smartfactory.worker.device.Device;
import com.example.smartfactory.worker.device.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IotDataService {

    private final DeviceRepository deviceRepository;

    private final IotDataRepository iotDataRepository;

    @Transactional
    public UUID save(IotMessagePayload payload) {
        Device device = deviceRepository.findByDeviceId(payload.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unknown device: " + payload.deviceId()));

        iotDataRepository.insertWithOnConflictDoNothing(
                payload.deviceId(),
                device.getUserId(),
                payload.temperature(),
                payload.humidity(),
                payload.motion(),
                payload.powerW(),
                payload.recordedAt());

        return device.getUserId();
    }
}
