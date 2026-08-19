package com.samuelfilho_dev.finance_module.auth.controllers;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthResponse;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.ResetMfaRequest;
import com.samuelfilho_dev.finance_module.auth.services.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "api/{version}/auth", version = "1")
public class AuthController {
    public final LoginService loginService;

    @PostMapping("login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody CreateLoginRequest payload) {
        return ResponseEntity.ok(loginService.login(payload));
    }

    @PostMapping("mfa/enable")
    public ResponseEntity<AuthResponse> enableMfaFactor(@Valid @RequestBody MfaRequest payload) {
        this.loginService.enableMfaFactor(payload);

        return ResponseEntity.ok().body(new AuthResponse(
                true,
                "2FA Ativado com sucesso",
                "/api/v1/auth/login",
                null
        ));
    }

    @PostMapping("mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfaFactor(@Valid @RequestBody MfaRequest payload) {
        return ResponseEntity.ok((this.loginService.verifyMfaFactor(payload)));
    }

    @PostMapping("mfa/reset")
    public ResponseEntity<AuthResponse> resetMfaFactor(@Valid @RequestBody ResetMfaRequest payload) {
        return ResponseEntity.ok((this.loginService.resetMfaFactor(payload)));
    }
}
