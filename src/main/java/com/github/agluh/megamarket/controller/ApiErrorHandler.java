package com.github.agluh.megamarket.controller;

import com.github.agluh.megamarket.dto.ApiError;
import com.github.agluh.megamarket.service.exceptions.IdentityIsNotUniqueException;
import com.github.agluh.megamarket.service.exceptions.ShopUnitNotFoundException;
import java.time.format.DateTimeParseException;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Error handler for REST API.
 */
@ControllerAdvice
@Slf4j
public class ApiErrorHandler {

    @ExceptionHandler({ ShopUnitNotFoundException.class, NoHandlerFoundException.class })
    protected ResponseEntity<Object> handleResourceNotFound(Throwable ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiError apiError = new ApiError(status.value(), "Item not found");
        log.info("item not found", ex);
        return new ResponseEntity<>(apiError, status);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class, ConstraintViolationException.class,
        IdentityIsNotUniqueException.class, DateTimeParseException.class,
        MissingServletRequestParameterException.class, IllegalArgumentException.class })
    protected ResponseEntity<Object> handleMalformedRequest(Throwable ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError apiError = new ApiError(status.value(), "Validation failed");
        log.info("validation failed", ex);
        return new ResponseEntity<>(apiError, status);
    }
}
