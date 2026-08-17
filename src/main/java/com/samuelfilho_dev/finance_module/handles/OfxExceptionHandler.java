package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.OfxException;
import com.samuelfilho_dev.finance_module.exceptions.dtos.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class OfxExceptionHandler {
    @ExceptionHandler(OfxException.class)
    public ResponseEntity<ApiError> handleOfxParseException(OfxException ex) {
        var status = HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(ApiError.of(status, ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        var status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiError.of(status, "Parâmetro obrigatório ausente: " + ex.getRequestPartName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        var status = HttpStatus.CONTENT_TOO_LARGE;
        return ResponseEntity.status(status).body(ApiError.of(status, "O tamanho do arquivo excede o limite permitido."));
    }
}
