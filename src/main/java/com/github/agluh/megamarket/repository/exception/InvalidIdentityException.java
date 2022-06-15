package com.github.agluh.megamarket.repository.exception;

/**
 * This error being thrown in case category or offer refers not a valid category as parent.
 */
public class InvalidIdentityException extends RuntimeException {

    public InvalidIdentityException(Throwable cause) {
        super(cause);
    }
}
