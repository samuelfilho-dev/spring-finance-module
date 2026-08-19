package com.samuelfilho_dev.finance_module.auth.dtos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthResponseTest {

    @Test
    void compactConstructor_shouldLeaveQrFieldsNull() {
        var response = new AuthResponse(true, "ok", "/path", "token");

        assertEquals(true, response.success());
        assertEquals("ok", response.message());
        assertEquals("/path", response.path());
        assertEquals("token", response.token());
        assertNull(response.qrCodeBase64());
        assertNull(response.otpAuthUrl());
    }
}
