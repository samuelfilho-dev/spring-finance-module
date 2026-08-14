package com.samuelfilho_dev.finance_module.account.services.impl;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.account.mappers.BankAccountMapper;
import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.account.services.BankAccountService;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountImpl implements BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    private final BankAccountMapper bankAccountMapper;


    @Override
    public BankAccountResponse createBankAccount(CreateBankAccountRequest payload) {
        userRepository.findById(payload.userId())
                .orElseThrow(() -> new NoSuchElementException("Usuario não encontrado"));

        var newBankAccount = BankAccount.builder()
                .bankName(this.normalizeBankAccountName(payload.bankName()))
                .agency(payload.agency())
                .accountNumber(payload.accountNumber())
                .balance(Objects.requireNonNullElse(payload.balance(), BigDecimal.ZERO))
                .status(BankAccountStatus.ACTIVE)
                .userId(new ObjectId(payload.userId()))
                .build();

        var bankAccount = bankAccountRepository.save(newBankAccount);

        log.info("Conta Bancaria criada com ID {}", bankAccount.getId());
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public List<BankAccountResponse> findAllBankAccounts() {
        var bankAccounts = bankAccountRepository
                .findAll()
                .stream()
                .filter(bankAccount -> bankAccount.getStatus() == BankAccountStatus.ACTIVE)
                .toList();

        log.info("Contas Bancarias foram listadas");
        return bankAccountMapper.toResponseList(bankAccounts);
    }

    @Override
    public BankAccountResponse findBankAccountById(String id) {
        var bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Conta Bancaria não encontrada"));

        log.info("Conta Bancaria com id {} foi encontrada", bankAccount.getId());
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public BankAccountResponse updateBankAccount(String id, UpdateBankAccountRequest payload) {
        var bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Conta Bancaria não encontrada"));

        bankAccount.setBankName(payload.bankName());
        bankAccount.setAgency(payload.agency());
        bankAccount.setAccountNumber(payload.accountNumber());

        bankAccountRepository.save(bankAccount);

        log.info("Conta Bancaria foi atulizada");
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public void deleteBankAccount(String id) {
        var bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Conta Bancaria não encontrada"));

        bankAccount.setStatus(BankAccountStatus.INACTIVE);

        log.info("Conta Bancaria foi deletada");
        bankAccountRepository.save(bankAccount);
    }

    private String normalizeBankAccountName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome do Banco é requerido");
        }

        var normalized = name
                .trim()
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .toUpperCase()
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        return normalized.startsWith("BANCO_")
                ? normalized
                : "BANCO_" + normalized;
    }
}
