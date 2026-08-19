package com.samuelfilho_dev.finance_module.auth.controllers;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthResponse;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.ResetMfaRequest;
import com.samuelfilho_dev.finance_module.auth.services.LoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private LoginService loginService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_shouldReturnServiceResponse() {
        var payload = new CreateLoginRequest("user@test.com", "secret");
        var body = new AuthResponse(true, "ok", "/path", "token");
        when(loginService.login(payload)).thenReturn(body);

        var response = authController.login(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body, response.getBody());
    }

    @Test
    void enableMfaFactor_shouldReturnSuccessPayload() {
        var payload = new MfaRequest("user@test.com", "123456");

        var response = authController.enableMfaFactor(payload);

        verify(loginService).enableMfaFactor(payload);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().success());
        assertEquals("2FA Ativado com sucesso", response.getBody().message());
        assertEquals("/api/v1/auth/login", response.getBody().path());
    }

    @Test
    void verifyMfaFactor_shouldReturnServiceResponse() {
        var payload = new MfaRequest("user@test.com", "123456");
        var body = new AuthResponse(true, "MFA verificado com sucesso", null, "access");
        when(loginService.verifyMfaFactor(payload)).thenReturn(body);

        var response = authController.verifyMfaFactor(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body, response.getBody());
    }

    @Test
    void resetMfaFactor_shouldReturnServiceResponse() {
        var payload = new ResetMfaRequest("user@test.com");
        var body = new AuthResponse(true, "2FA resetado com sucesso", null, null);
        when(loginService.resetMfaFactor(payload)).thenReturn(body);

        var response = authController.resetMfaFactor(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body, response.getBody());
    }
}
