package com.nexora.auth.exception;

/**
 * Thrown when someone with CORRECT credentials tries to log in, but
 * their account status isn't ACTIVE (e.g. SUSPENDED by an admin).
 * Unlike InvalidCredentialsException, it's fine to be specific here —
 * this only fires after the password has already been verified
 * correct, so there's no user-enumeration risk in telling them why.
 */
public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String status) {
        super("Account is not active (current status: " + status + ")");
    }
}
