package com.nexora.auth.exception;

/**
 * Thrown for a failed login: wrong password OR unknown email.
 *
 * IMPORTANT SECURITY DETAIL: notice that AuthService throws this SAME
 * exception, with the SAME generic message, whether the email doesn't
 * exist at all or the password is simply wrong. If we told the client
 * "no such email" vs "wrong password" as different errors, we'd be
 * handing an attacker a free tool to discover which email addresses
 * are registered on Nexora (a "user enumeration" vulnerability).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
