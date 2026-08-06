package com.samuelfilho_dev.finance_module.account.services;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse createBankAccount(CreateBankAccountRequest payload);

    List<BankAccountResponse> findAllBankAccounts();

    BankAccountResponse findBankAccountById(String id);

    BankAccountResponse updateBankAccount(String id, UpdateBankAccountRequest payload);

    void deleteBankAccount(String id);
}
