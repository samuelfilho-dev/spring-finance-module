package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.OfxException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfxExceptionHandlerTest {

    private final OfxExceptionHandler handler = new OfxExceptionHandler();

    @Test
    void handleOfxParseException_shouldReturnUnprocessableContent() {
        var response = handler.handleOfxParseException(new OfxException("arquivo inválido"));
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
        assertEquals("arquivo inválido", response.getBody().message());
    }

    @Test
    void handleMissingServletRequestPartException_shouldReturnBadRequest() {
        var response = handler.handleMissingServletRequestPartException(new MissingServletRequestPartException("file"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parâmetro obrigatório ausente: file", response.getBody().message());
    }

    @Test
    void handleMaxUploadSizeExceededException_shouldReturnContentTooLarge() {
        var response = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(1024));
        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertEquals("O tamanho do arquivo excede o limite permitido.", response.getBody().message());
    }
}
