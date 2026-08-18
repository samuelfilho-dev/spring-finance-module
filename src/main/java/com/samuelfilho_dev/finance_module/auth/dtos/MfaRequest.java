package com.samuelfilho_dev.finance_module.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaRequest(
        @NotBlank(message = "Email é requerido")
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Código deve conter 6 dígitos")
        String code
) {
}
