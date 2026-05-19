package com.example.smartfactory.api.user;

import com.example.smartfactory.api.user.dto.UserResponse;
import com.example.smartfactory.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteMe(UUID userId) {
        // シーケンス図の削除順序に従い、同一トランザクション内で関連データを明示的に削除する
        userRepository.deleteReportDownloadsByUserId(userId);
        userRepository.deleteDailyReportsByUserId(userId);
        userRepository.deleteAnomalyLogsByUserId(userId);
        userRepository.deleteAnomalyThresholdsByUserId(userId);
        userRepository.deleteIotDataByUserId(userId);
        userRepository.deleteDevicesByUserId(userId);
        userRepository.deleteById(userId);
    }
}
