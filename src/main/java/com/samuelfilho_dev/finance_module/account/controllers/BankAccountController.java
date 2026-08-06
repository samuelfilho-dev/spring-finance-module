package com.samuelfilho_dev.finance_module.account.controllers;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.services.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "api/{version}/accounts", version = "1")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> getBankAccounts() {
        return ResponseEntity.ok(bankAccountService.findAllBankAccounts());
    }

    @GetMapping("{id}")
    public ResponseEntity<BankAccountResponse> getBankAccount(@PathVariable String id) {
        return ResponseEntity.ok(bankAccountService.findBankAccountById(id));
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> createBankAccount(@Valid @RequestBody CreateBankAccountRequest payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountService.createBankAccount(payload));
    }

    @PutMapping("{id}")
    public ResponseEntity<BankAccountResponse> updateBankAccount(@PathVariable String id,
                                                                 @Valid @RequestBody UpdateBankAccountRequest payload) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(id, payload));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable String id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }
}
