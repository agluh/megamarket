package com.github.agluh.megamarket.service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This error being thrown if no catalog item could be found by identity.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShopUnitNotFoundException extends RuntimeException {

}
