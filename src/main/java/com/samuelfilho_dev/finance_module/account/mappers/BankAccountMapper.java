package com.samuelfilho_dev.finance_module.account.mappers;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {
    BankAccountResponse toResponse(BankAccount bankAccount);

    List<BankAccountResponse> toResponseList(List<BankAccount> bankAccounts);
}
