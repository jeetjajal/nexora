package com.nexora.auth.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("Account is not active");
    }

    public AccountNotActiveException(String message) {
        super(message);
    }
}