package com.example.smartfactory.api.device;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.device.dto.DeviceRequest;
import com.example.smartfactory.api.device.dto.DeviceResponse;
import com.example.smartfactory.api.device.dto.DeviceUpdateRequest;
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
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getDevices(HttpSession session) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(deviceService.getDevices(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(
            HttpSession session,
            @Valid @RequestBody DeviceRequest request) {
        UUID userId = currentUserId(session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(deviceService.createDevice(userId, request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            HttpSession session,
            @PathVariable UUID id,
            @Valid @RequestBody DeviceUpdateRequest request) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(deviceService.updateDevice(userId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(HttpSession session, @PathVariable UUID id) {
        UUID userId = currentUserId(session);
        deviceService.deleteDevice(userId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
