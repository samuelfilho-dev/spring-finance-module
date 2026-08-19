package com.samuelfilho_dev.finance_module.handles;

import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void handleBadCredentialsException_shouldReturnUnauthorizedWithGenericMessage() {
        var response = handler.handleBadCredentialsException(new BadCredentialsException("secret"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email ou senha incorreto", response.getBody().message());
    }

    @Test
    void handleUnauthorizedException_shouldReturnUnauthorized() {
        var response = handler.handleUnauthorizedException(new UnauthorizedException("Falha na validação do MFA"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Falha na validação do MFA", response.getBody().message());
    }

    @Test
    void handleForbiddenException_shouldReturnForbidden() {
        var response = handler.handleForbiddenException(new ForbiddenException("sem permissão"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("sem permissão", response.getBody().message());
    }
}
