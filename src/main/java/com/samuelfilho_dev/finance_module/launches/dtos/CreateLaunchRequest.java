package com.samuelfilho_dev.finance_module.launches.dtos;

import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.validators.ObjectId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateLaunchRequest(
        @NotBlank(message = "Titulo do lançamento é requerido")
        String title,

        String description,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @NotNull(message = "Data do lançamento é requerido")
        Instant launchDate,

        @NotNull(message = "Valor do lançamento é requerido")
        @PositiveOrZero(message = "Valor do laçamento deve ser maior ou igual a zero")
        BigDecimal amount,

        @NotNull(message = "Tipo do Lançamento é requerido")
        LaunchType type,

        @NotBlank(message = "ID da Conta Bancaria é requerida")
        @ObjectId
        String bankAccountId
) {
}
