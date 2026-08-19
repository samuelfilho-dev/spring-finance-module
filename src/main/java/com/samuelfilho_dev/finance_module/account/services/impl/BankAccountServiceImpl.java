package com.samuelfilho_dev.finance_module.account.services.impl;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.account.mappers.BankAccountMapper;
import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.account.services.BankAccountService;
import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountServiceImpl implements BankAccountService {
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    private final BankAccountRepository bankAccountRepository;

    private final BankAccountMapper bankAccountMapper;


    @Override
    public BankAccountResponse createBankAccount(CreateBankAccountRequest payload) {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var newBankAccount = BankAccount.builder()
                .bankName(this.normalizeBankAccountName(payload.bankName()))
                .agency(payload.agency())
                .accountNumber(payload.accountNumber())
                .balance(Objects.requireNonNullElse(payload.balance(), BigDecimal.ZERO))
                .status(BankAccountStatus.ACTIVE)
                .userId(new ObjectId(Objects.requireNonNull(user).getId()))
                .build();

        var bankAccount = bankAccountRepository.save(newBankAccount);

        log.info("Conta Bancaria criada com ID {}", bankAccount.getId());
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public List<BankAccountResponse> findAllBankAccounts() {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var bankAccounts = bankAccountRepository
                .findAll()
                .stream()
                .filter(bankAccount -> bankAccount.getStatus() == BankAccountStatus.ACTIVE)
                .filter(bankAccount -> {
                    var userId = bankAccount.getUserId();
                    return userId != null && userId.toString().equals(Objects.requireNonNull(user).getId());
                })
                .toList();

        log.info("Contas Bancarias do usuario {} foram encontradas", user.getId());
        return bankAccountMapper.toResponseList(bankAccounts);
    }

    @Override
    public BankAccountResponse findBankAccountById(String id) {
        var bankAccount = this.getBankAccount(id);

        log.info("Conta Bancaria com id {} foi encontrada", bankAccount.getId());
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public BankAccountResponse updateBankAccount(String id, UpdateBankAccountRequest payload) {
        var bankAccount = this.getBankAccount(id);

        bankAccount.setBankName(payload.bankName());
        bankAccount.setAgency(payload.agency());
        bankAccount.setAccountNumber(payload.accountNumber());

        bankAccountRepository.save(bankAccount);

        log.info("Conta Bancaria foi atulizada");
        return bankAccountMapper.toResponse(bankAccount);
    }

    @Override
    public void deleteBankAccount(String id) {
        var bankAccount = this.getBankAccount(id);

        bankAccount.setStatus(BankAccountStatus.INACTIVE);

        log.info("Conta Bancaria foi deletada");
        bankAccountRepository.save(bankAccount);
    }

    private BankAccount getBankAccount(String id) {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Conta Bancaria não encontrada"));

        if (!bankAccount.getUserId().toString().equals(Objects.requireNonNull(user).getId())) {
            log.warn("Usuario {} tentou acessar a conta bancaria {} sem permissão", user.getId(), bankAccount.getId());
            throw new ForbiddenException("Você não tem permissão para acessar esta conta bancária");
        }

        return bankAccount;
    }

    private String normalizeBankAccountName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Nome do Banco é requerido");
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
