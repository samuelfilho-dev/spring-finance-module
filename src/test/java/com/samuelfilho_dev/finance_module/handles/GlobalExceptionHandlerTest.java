package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidException_shouldReturnBadRequestWithFieldErrors() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
        bindingResult.addError(new FieldError("payload", "email", "E-mail é requerido"));
        var exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        var response = handler.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().message().contains("email: E-mail é requerido"));
    }

    @Test
    void handleMethodArgumentNotValidException_shouldJoinMultipleFieldErrors() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
        bindingResult.addError(new FieldError("payload", "email", "E-mail é requerido"));
        bindingResult.addError(new FieldError("payload", "name", "Nome é requerido"));
        var exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        var response = handler.handleMethodArgumentNotValidException(exception);

        assertTrue(response.getBody().message().contains("email: E-mail é requerido"));
        assertTrue(response.getBody().message().contains("name: Nome é requerido"));
    }

    @Test
    void handleHttpMessageNotReadableException_shouldReturnBadRequest() {
        var exception = new HttpMessageNotReadableException("JSON inválido", (HttpInputMessage) null);
        var response = handler.handleHttpMessageNotReadableException(exception);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("JSON inválido", response.getBody().message());
    }

    @Test
    void handleBusinessException_shouldReturnBadRequest() {
        var response = handler.handleBusinessException(new BusinessException("Email já cadastrado"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email já cadastrado", response.getBody().message());
    }

    @Test
    void notFoundExceptionHandler_shouldReturnNotFound() {
        var response = handler.notFoundExceptionHandler(new NotFoundException("Usuário não encontrado"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Usuário não encontrado", response.getBody().message());
    }

    @Test
    void handleUnexpectedHandler_shouldHideInternalDetails() {
        var response = handler.handleUnexpectedHandler(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno inesperado.", response.getBody().message());
    }

    private static MethodParameter methodParameter() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }
}
