package com.samuelfilho_dev.finance_module.auth.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        Boolean success,
        String message,
        String path,
        String token,
        String qrCodeBase64,
        String otpAuthUrl
) {
    public AuthResponse(Boolean success, String message, String path, String token) {
        this(success, message, path, token, null, null);
    }
}
