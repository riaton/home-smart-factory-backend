package com.example.smartfactory.api.user;

import com.example.smartfactory.api.auth.AuthService;
import com.example.smartfactory.api.user.dto.UserResponse;
import com.example.smartfactory.common.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(HttpSession session) {
        UUID userId = currentUserId(session);
        return ResponseEntity.ok(ApiResponse.of(userService.getMe(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(HttpSession session) {
        UUID userId = currentUserId(session);
        userService.deleteMe(userId);
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(HttpSession session) {
        return UUID.fromString((String) session.getAttribute(AuthService.SESSION_KEY_USER_ID));
    }
}
