package com.samuelfilho_dev.finance_module.users.dtos;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;

import java.util.List;

public record UserResponse(
        String id,
        String name,
        String email,
        AddressResponse address,
        List<BankAccountResponse> accounts
) {

}
