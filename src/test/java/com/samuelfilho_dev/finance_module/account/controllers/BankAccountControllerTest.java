package com.samuelfilho_dev.finance_module.account.controllers;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.services.BankAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountControllerTest {

    private static final String ACCOUNT_ID = "665f1c2e8f1a2b3c4d5e6f7a";

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private BankAccountController bankAccountController;

    @Test
    void getBankAccounts_shouldReturnOkWithServiceResult() {
        var accounts = List.of(sampleResponse());
        when(bankAccountService.findAllBankAccounts()).thenReturn(accounts);

        var response = bankAccountController.getBankAccounts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(accounts, response.getBody());
    }

    @Test
    void getBankAccount_shouldReturnOkWithAccount() {
        var account = sampleResponse();
        when(bankAccountService.findBankAccountById(ACCOUNT_ID)).thenReturn(account);

        var response = bankAccountController.getBankAccount(ACCOUNT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(account, response.getBody());
    }

    @Test
    void createBankAccount_shouldReturnCreatedWithCreatedAccount() {
        var payload = new CreateBankAccountRequest("Nubank", "0001", "12345-6", BigDecimal.TEN);
        var created = sampleResponse();
        when(bankAccountService.createBankAccount(payload)).thenReturn(created);

        var response = bankAccountController.createBankAccount(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(created, response.getBody());
    }

    @Test
    void updateBankAccount_shouldReturnOkWithUpdatedAccount() {
        var payload = new UpdateBankAccountRequest("BANCO_ITAU", "0002", "99999-0");
        var updated = new BankAccountResponse(ACCOUNT_ID, "BANCO_ITAU", "0002", "99999-0", "10");
        when(bankAccountService.updateBankAccount(ACCOUNT_ID, payload)).thenReturn(updated);

        var response = bankAccountController.updateBankAccount(ACCOUNT_ID, payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
    }

    @Test
    void deleteBankAccount_shouldReturnNoContent() {
        var response = bankAccountController.deleteBankAccount(ACCOUNT_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(bankAccountService).deleteBankAccount(ACCOUNT_ID);
    }

    private static BankAccountResponse sampleResponse() {
        return new BankAccountResponse(ACCOUNT_ID, "BANCO_NUBANK", "0001", "12345-6", "10");
    }
}
