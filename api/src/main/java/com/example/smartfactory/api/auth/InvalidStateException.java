package com.example.smartfactory.api.auth;

/** OAuth コールバックの state パラメータ不一致時の例外（HTTP 400）。 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException() {
        super("state パラメータが不正です");
    }
}
