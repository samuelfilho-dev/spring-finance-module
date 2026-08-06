package com.samuelfilho_dev.finance_module.account.dtos;

public record BankAccountResponse(
        String id,
        String bankName,
        String agency,
        String accountNumber,
        String balance
) {
}
