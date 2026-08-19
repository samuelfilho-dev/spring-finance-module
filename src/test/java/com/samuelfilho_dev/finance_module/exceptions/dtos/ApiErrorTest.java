package com.samuelfilho_dev.finance_module.exceptions.dtos;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiErrorTest {

    @Test
    void of_shouldJoinFieldErrors() {
        var error = ApiError.of(HttpStatus.BAD_REQUEST, List.of("email: requerido", "name: requerido"));

        assertEquals(400, error.status());
        assertEquals("Bad Request", error.error());
        assertEquals("email: requerido, name: requerido", error.message());
        assertNotNull(error.timestamp());
    }

    @Test
    void of_shouldKeepSingleMessage() {
        var error = ApiError.of(HttpStatus.NOT_FOUND, "Usuário não encontrado");

        assertEquals(404, error.status());
        assertEquals("Not Found", error.error());
        assertEquals("Usuário não encontrado", error.message());
    }
}
