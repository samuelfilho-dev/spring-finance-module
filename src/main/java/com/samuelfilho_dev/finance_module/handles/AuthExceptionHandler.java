package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.UnauthorizedException;
import com.samuelfilho_dev.finance_module.exceptions.dtos.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorizedException(UnauthorizedException e) {
        var status = HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(ApiError.of(status, e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbiddenException(ForbiddenException e) {
        var status = HttpStatus.FORBIDDEN;
        return ResponseEntity.status(status).body(ApiError.of(status, e.getMessage()));
    }
}
