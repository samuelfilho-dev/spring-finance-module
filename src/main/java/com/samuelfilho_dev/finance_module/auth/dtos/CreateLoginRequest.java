package com.samuelfilho_dev.finance_module.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateLoginRequest(
        @NotBlank(message = "Email é requerido")
        String email,

        @NotBlank(message = "Senha é requerida")
        String password
) {
}
