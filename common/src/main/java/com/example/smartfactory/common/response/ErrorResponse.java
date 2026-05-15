package com.example.smartfactory.common.response;

/** エラーレスポンスの共通ラッパー。{"error": {"code": "...", "message": "..."}} 形式で返す。 */
public record ErrorResponse(ErrorDetail error) {

    /** エラー詳細。 */
    public record ErrorDetail(String code, String message) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message));
    }
}
