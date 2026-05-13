package com.example.smartfactory.common.exception;

/** リソースの重複登録時の例外（HTTP 409）。 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
