package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.exceptions.dtos.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        var status = org.springframework.http.HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.of(status, ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFoundExceptionHandler(NotFoundException ex) {
        var status = org.springframework.http.HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ApiError.of(status, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedHandler(Exception ex) {
        var status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiError.of(status, "Erro interno inesperado."));
    }
}
