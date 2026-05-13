package com.example.smartfactory.common.exception;

/** レポートダウンロード上限超過時の例外（HTTP 429）。 */
public class DownloadLimitExceededException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "本日のダウンロード上限（3回）を超過しました";

    public DownloadLimitExceededException() {
        super(DEFAULT_MESSAGE);
    }

    public DownloadLimitExceededException(String message) {
        super(message);
    }
}
