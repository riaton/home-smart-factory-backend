package com.example.smartfactory.api.iotdata;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.iotdata.dto.IotDataResponse;
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
@RequestMapping("/api/iot-data")
@RequiredArgsConstructor
public class IotDataController {

    private final IotDataService iotDataService;

    @GetMapping
    public ResponseEntity<PagedResponse<IotDataResponse>> getIotData(
            HttpSession session,
            @RequestParam(name = "device_id", required = false) String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "100") int perPage) {
        return ResponseEntity.ok(iotDataService.findAll(currentUserId(session), deviceId, from, to, page, perPage));
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
