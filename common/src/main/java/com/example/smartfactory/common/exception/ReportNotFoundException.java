package com.example.smartfactory.common.exception;

/** レポート未検出時の例外（HTTP 404）。他ユーザーのレポートへのアクセスも同一例外で返す。 */
public class ReportNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "レポートが見つかりません";

    public ReportNotFoundException() {
        super(DEFAULT_MESSAGE);
    }
}
