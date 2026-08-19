package com.samuelfilho_dev.finance_module.account.dtos;

import com.samuelfilho_dev.finance_module.validators.ObjectId;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateBankAccountRequest(
        @NotBlank(message = "Nome do Banco é requerido")
        String bankName,

        @NotBlank(message = "Agência é requerida")
        String agency,

        @NotBlank(message = "Número da Conta é requerido")
        String accountNumber,

        BigDecimal balance
) {
}
