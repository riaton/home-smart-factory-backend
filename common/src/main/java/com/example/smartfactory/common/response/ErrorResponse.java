package com.example.smartfactory.common.response;

/**
 * エラーレスポンスの共通ラッパー。{"error": "..."} 形式で返す。
 */
public record ErrorResponse(String error) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }
}
