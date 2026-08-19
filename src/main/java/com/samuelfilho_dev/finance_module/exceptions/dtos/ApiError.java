package com.samuelfilho_dev.finance_module.exceptions.dtos;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message
) {
    public static ApiError of(HttpStatus status, List<String> errors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), String.join(", ", errors));
    }

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
