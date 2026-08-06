package com.samuelfilho_dev.finance_module.account.dtos;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateBankAccountRequest(
        String bankName,
        String agency,
        String accountNumber
) {
}
