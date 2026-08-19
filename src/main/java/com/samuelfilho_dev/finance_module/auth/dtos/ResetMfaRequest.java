package com.samuelfilho_dev.finance_module.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record ResetMfaRequest(
        @NotEmpty(message = "Email é requerido")
        @Email(message = "Email inválido")
        String email
) {
}
