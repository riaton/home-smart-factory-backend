package com.example.smartfactory.common.response;

/**
 * 成功レスポンスの共通ラッパー。{"data": ...} 形式で返す。
 *
 * @param <T> レスポンスボディの型
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
