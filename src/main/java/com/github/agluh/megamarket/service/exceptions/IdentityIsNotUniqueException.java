package com.github.agluh.megamarket.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This error being thrown in case of duplications in IDs during import operation.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IdentityIsNotUniqueException extends RuntimeException {

}
