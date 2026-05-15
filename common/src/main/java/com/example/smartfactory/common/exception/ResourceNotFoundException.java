package com.example.smartfactory.common.exception;

/** リソースが見つからない場合の例外（HTTP 404）。 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
