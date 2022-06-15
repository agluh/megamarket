package com.github.agluh.megamarket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO for API errors.
 */
@AllArgsConstructor
@Getter
public class ApiError {

    private final int code;

    private final String message;
}
