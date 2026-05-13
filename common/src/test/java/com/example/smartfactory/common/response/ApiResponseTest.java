package com.example.smartfactory.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("of() で data フィールドにラップされること")
    void of_wrapsDataField() {
        ApiResponse<String> response = ApiResponse.of("hello");

        assertThat(response.data()).isEqualTo("hello");
    }

    @Test
    @DisplayName("null データも受け入れること")
    void of_acceptsNullData() {
        ApiResponse<String> response = ApiResponse.of(null);

        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("ErrorResponse.of() でエラーメッセージがラップされること")
    void errorResponse_of_wrapsErrorField() {
        ErrorResponse response = ErrorResponse.of("エラーが発生しました");

        assertThat(response.error()).isEqualTo("エラーが発生しました");
    }
}
