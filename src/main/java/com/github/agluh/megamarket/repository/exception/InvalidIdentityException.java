package com.github.agluh.megamarket.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This error being thrown in case category or offer refers not a valid category as parent.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidIdentityException extends RuntimeException {

    public InvalidIdentityException(Throwable cause) {
        super(cause);
    }
}
