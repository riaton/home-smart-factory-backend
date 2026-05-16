package com.example.smartfactory.worker.iotdata;

import com.example.smartfactory.common.exception.ResourceNotFoundException;
import com.example.smartfactory.worker.device.Device;
import com.example.smartfactory.worker.device.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class IotDataServiceTest {

    private static final String DEVICE_ID = "room01";

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private IotDataRepository iotDataRepository;

    private IotDataService iotDataService;

    @BeforeEach
    void setup() {
        iotDataService = new IotDataService(deviceRepository, iotDataRepository);
    }

    @Test
    @DisplayName("save は device を取得して iot_data を INSERT すること")
    void save_validPayload_insertsIotData() {
        IotMessagePayload payload = new IotMessagePayload(
                DEVICE_ID,
                new BigDecimal("25.30"),
                new BigDecimal("60.10"),
                1,
                new BigDecimal("120.50"),
                OffsetDateTime.parse("2026-01-15T10:00:00+09:00"));
        Device device = mock(Device.class);
        given(device.getUserId()).willReturn(USER_ID);
        given(deviceRepository.findByDeviceId(DEVICE_ID)).willReturn(Optional.of(device));

        iotDataService.save(payload);

        then(iotDataRepository).should().insertWithOnConflictDoNothing(
                eq(DEVICE_ID),
                eq(USER_ID),
                eq(new BigDecimal("25.30")),
                eq(new BigDecimal("60.10")),
                eq(1),
                eq(new BigDecimal("120.50")),
                any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("save で未登録デバイスの場合 ResourceNotFoundException をスローすること")
    void save_unknownDevice_throwsResourceNotFoundException() {
        IotMessagePayload payload = new IotMessagePayload(
                "unknown-device", null, null, null, null,
                OffsetDateTime.parse("2026-01-15T10:00:00+09:00"));
        given(deviceRepository.findByDeviceId("unknown-device")).willReturn(Optional.empty());

        assertThatThrownBy(() -> iotDataService.save(payload))
                .isInstanceOf(ResourceNotFoundException.class);

        then(iotDataRepository).shouldHaveNoInteractions();
    }
}
