package com.samuelfilho_dev.finance_module.users.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        String id,
        String name,
        String email,
        AddressResponse address,
        List<BankAccountResponse> accounts
) {

}
