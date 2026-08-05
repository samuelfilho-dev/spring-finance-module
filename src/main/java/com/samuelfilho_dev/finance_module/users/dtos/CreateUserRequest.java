package com.samuelfilho_dev.finance_module.users.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotNull(message = "Nome é requerido")
        String name,

        @NotBlank(message = "E-mail é requerido")
        @Email(message = "E-mail não Válido")
        String email,

        String password,

        @Valid
        AddressRequest address
) {
}
