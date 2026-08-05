package com.samuelfilho_dev.finance_module.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Logradouro é requerido")
        String street,

        @NotBlank(message = "Número é requerido")
        String number,

        String complement,

        @NotBlank(message = "Cidade é requerida")
        String city,

        @NotBlank(message = "UF é requerido")
        @Size(min = 2, max = 2, message = "Estado no máximo 2 caracteres")
        String state,

        @NotBlank(message = "CEP é requerido")
        @Size(min = 8, max = 8, message = "CEP no máximo 8 caracteres")
        String postalCode
) {
}
