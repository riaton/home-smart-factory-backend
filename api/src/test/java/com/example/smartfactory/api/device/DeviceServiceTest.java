package com.example.smartfactory.api.device;

import com.example.smartfactory.api.device.dto.DeviceRequest;
import com.example.smartfactory.api.device.dto.DeviceResponse;
import com.example.smartfactory.api.device.dto.DeviceUpdateRequest;
import com.example.smartfactory.common.exception.DuplicateResourceException;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceService deviceService;

    @BeforeEach
    void setup() {
        deviceService = new DeviceService(deviceRepository);
    }

    @Test
    @DisplayName("getDevices はユーザーのデバイス一覧を返すこと")
    void getDevices_returnsDeviceList() {
        Device device = mock(Device.class);
        given(device.getId()).willReturn(DEVICE_ID);
        given(device.getDeviceId()).willReturn("room01");
        given(device.getName()).willReturn("リビング");
        given(device.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).willReturn(List.of(device));

        List<DeviceResponse> result = deviceService.getDevices(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceId()).isEqualTo("room01");
        assertThat(result.get(0).name()).isEqualTo("リビング");
    }

    @Test
    @DisplayName("createDevice は登録済みデバイスを DeviceResponse で返すこと")
    void createDevice_returnsDeviceResponse() {
        DeviceRequest request = new DeviceRequest("room01", "リビング");
        Device device = mock(Device.class);
        given(device.getId()).willReturn(DEVICE_ID);
        given(device.getDeviceId()).willReturn("room01");
        given(device.getName()).willReturn("リビング");
        given(device.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceRepository.saveAndFlush(any())).willReturn(device);

        DeviceResponse result = deviceService.createDevice(USER_ID, request);

        assertThat(result.deviceId()).isEqualTo("room01");
        assertThat(result.name()).isEqualTo("リビング");
    }

    @Test
    @DisplayName("createDevice で device_id が重複した場合 DuplicateResourceException をスローすること")
    void createDevice_duplicateDeviceId_throwsDuplicateResourceException() {
        DeviceRequest request = new DeviceRequest("room01", "リビング");
        given(deviceRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> deviceService.createDevice(USER_ID, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("updateDevice はデバイス名を更新した DeviceResponse を返すこと")
    void updateDevice_returnsUpdatedDeviceResponse() {
        DeviceUpdateRequest request = new DeviceUpdateRequest("寝室");
        Device device = mock(Device.class);
        given(device.getId()).willReturn(DEVICE_ID);
        given(device.getDeviceId()).willReturn("room01");
        given(device.getName()).willReturn("寝室");
        given(device.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(deviceRepository.findByIdAndUserId(DEVICE_ID, USER_ID)).willReturn(Optional.of(device));

        DeviceResponse result = deviceService.updateDevice(USER_ID, DEVICE_ID, request);

        assertThat(result.name()).isEqualTo("寝室");
        then(device).should().updateName("寝室");
    }

    @Test
    @DisplayName("updateDevice でデバイスが存在しない場合 ResourceNotFoundException をスローすること")
    void updateDevice_deviceNotFound_throwsResourceNotFoundException() {
        DeviceUpdateRequest request = new DeviceUpdateRequest("寝室");
        given(deviceRepository.findByIdAndUserId(DEVICE_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.updateDevice(USER_ID, DEVICE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteDevice はシーケンス図の順序で関連データを削除すること")
    void deleteDevice_deletesInOrder() {
        Device device = mock(Device.class);
        given(device.getDeviceId()).willReturn("room01");
        given(deviceRepository.findByIdAndUserId(DEVICE_ID, USER_ID)).willReturn(Optional.of(device));
        InOrder inOrder = Mockito.inOrder(deviceRepository);

        deviceService.deleteDevice(USER_ID, DEVICE_ID);

        inOrder.verify(deviceRepository).deleteAnomalyLogsByDeviceId("room01");
        inOrder.verify(deviceRepository).deleteIotDataByDeviceId("room01");
        inOrder.verify(deviceRepository).deleteById(DEVICE_ID);
    }

    @Test
    @DisplayName("deleteDevice でデバイスが存在しない場合 ResourceNotFoundException をスローすること")
    void deleteDevice_deviceNotFound_throwsResourceNotFoundException() {
        given(deviceRepository.findByIdAndUserId(DEVICE_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice(USER_ID, DEVICE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
