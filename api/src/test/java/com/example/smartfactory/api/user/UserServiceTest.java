package com.example.smartfactory.api.user;

import com.example.smartfactory.api.user.dto.UserResponse;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("getMe はユーザー情報を UserResponse で返すこと")
    void getMe_returnsUserResponse() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        given(user.getEmail()).willReturn("user@example.com");
        given(user.getCreatedAt()).willReturn(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        UserResponse response = userService.getMe(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
    }

    @Test
    @DisplayName("getMe はユーザーが存在しない場合 ResourceNotFoundException をスローすること")
    void getMe_userNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteMe はシーケンス図の順序で全テーブルを削除すること")
    void deleteMe_deletesAllTablesInOrder() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        InOrder inOrder = Mockito.inOrder(userRepository);

        userService.deleteMe(userId);

        inOrder.verify(userRepository).deleteReportDownloadsByUserId(userId);
        inOrder.verify(userRepository).deleteDailyReportsByUserId(userId);
        inOrder.verify(userRepository).deleteAnomalyLogsByUserId(userId);
        inOrder.verify(userRepository).deleteAnomalyThresholdsByUserId(userId);
        inOrder.verify(userRepository).deleteIotDataByUserId(userId);
        inOrder.verify(userRepository).deleteDevicesByUserId(userId);
        inOrder.verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("deleteMe は deleteById を呼び出すこと")
    void deleteMe_callsDeleteById() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        userService.deleteMe(userId);

        then(userRepository).should().deleteById(userId);
    }
}
