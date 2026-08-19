package com.samuelfilho_dev.finance_module.launches.services.impl;

import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.launches.dtos.CreateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.UpdateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.mappers.LaunchMapper;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchRepository;
import com.samuelfilho_dev.finance_module.launches.services.LaunchService;
import com.samuelfilho_dev.finance_module.launches.utils.LaunchUtils;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchServiceImpl implements LaunchService {
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    private final LaunchRepository launchRepository;
    private final BankAccountRepository bankAccountRepository;

    private final LaunchMapper launchMapper;

    @Override
    public List<LaunchResponse> findAllLaunches() {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var launches = launchRepository
                .findAll()
                .stream()
                .filter(launch -> launch.getUserId().toString().equals(user.getId()))
                .toList();

        log.info("Foram encontrados {} lançamentos para o usuário {}", launches.size(), user.getId());
        return launchMapper.toResponseList(launches);
    }

    @Override
    public LaunchResponse findLaunchById(String id) {
        var launch = this.getLaunch(id);

        log.info("Lançamento com id {} foi encontrado", launch.getId());
        return launchMapper.toResponse(launch);
    }

    @Override
    public LaunchResponse createLaunch(CreateLaunchRequest payload) {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var bankAccount = bankAccountRepository.findById(payload.bankAccountId())
                .orElseThrow(() -> new NotFoundException("Conta Bancaria não encontrada"));

        if (!bankAccount.getUserId().toString().equals(user.getId())) {
            throw new ForbiddenException("Você não tem permissão para criar lançamentos nesta conta");
        }

        var launch = Launch.builder()
                .title(payload.title())
                .description(payload.description())
                .launchDate(payload.launchDate())
                .amount(payload.amount())
                .type(payload.type())
                .userId(new ObjectId(Objects.requireNonNull(user).getId()))
                .bankAccountId(new ObjectId(payload.bankAccountId()))
                .build();

        launchRepository.save(launch);

        log.info("Lançamento criado com sucesso");

        var newBalance = LaunchUtils.calculateNewBalance(
                launch.getType(),
                launch.getAmount(),
                bankAccount.getBalance()
        );
        bankAccount.setBalance(newBalance);
        bankAccountRepository.save(bankAccount);

        log.info("Saldo da conta bancária atualizado após criação do lançamento. launchId={}, bankAccountId={}",
                launch.getId(),
                bankAccount.getId());
        return launchMapper.toResponse(launch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LaunchResponse updateLaunch(String id, UpdateLaunchRequest payload) {
        var launch = this.getLaunch(id);

        var bankAccount = bankAccountRepository.findById(launch.getBankAccountId().toString())
                .orElseThrow(() -> new NotFoundException("Conta Bancaria não encontrada"));

        var oldType = launch.getType();
        var oldAmount = launch.getAmount();

        var newAmount = payload.amount() != null ? payload.amount() : oldAmount;
        var newType = payload.type() != null ? payload.type() : oldType;

        launch.setTitle(payload.title());
        launch.setDescription(payload.description());
        launch.setLaunchDate(payload.launchDate());
        launch.setAmount(payload.amount());
        launch.setType(payload.type());

        launchRepository.save(launch);

        log.info("O Lançamento com ID: {} foi atulizado", launch.getId());

        var isUpdateBalanceNeeded = newAmount.compareTo(oldAmount) != 0 || !newType.equals(oldType);

        if (isUpdateBalanceNeeded) {
            var reversedBalance = LaunchUtils.reverseBalance(oldType, oldAmount, bankAccount.getBalance());
            var newBalance = LaunchUtils.calculateNewBalance(newType, newAmount, reversedBalance);

            bankAccount.setBalance(newBalance);
            bankAccountRepository.save(bankAccount);

            log.info("Saldo da conta bancária atualizado após atualização do lançamento. launchId={}, bankAccountId={}",
                    launch.getId(),
                    bankAccount.getId());
            return launchMapper.toResponse(launch);
        }


        log.info("Lançamento atualizado sem alteração de valor/tipo — saldo mantido. launchId={}, bankAccountId={}",
                launch.getId(),
                bankAccount.getId());

        return launchMapper.toResponse(launch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLaunch(String id) {
        var launch = this.getLaunch(id);

        var bankAccount = bankAccountRepository.findById(launch.getBankAccountId().toString())
                .orElseThrow(() -> new NotFoundException("Conta Bancaria não encontrada"));

        var reversedBalance = LaunchUtils.reverseBalance(
                launch.getType(),
                launch.getAmount(),
                bankAccount.getBalance()
        );

        bankAccount.setBalance(reversedBalance);
        bankAccountRepository.save(bankAccount);

        log.info("Saldo da conta bancária atualizado após exclusão do lançamento. launchId={}, bankAccountId={}",
                launch.getId(),
                bankAccount.getId());
        this.launchRepository.delete(launch);
    }

    private Launch getLaunch(String id) {
        var auth = securityContextHolderStrategy.getContext().getAuthentication();
        var user = (AuthenticatedUser) Objects.requireNonNull(auth).getPrincipal();

        var launch = launchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));

        if (!launch.getUserId().toString().equals(Objects.requireNonNull(user).getId())) {
            log.warn("Usuario {} tentou acessar o lançamento {} sem permissão", user.getId(), launch.getId());
            throw new NotFoundException("Lançamento não encontrado");
        }

        return launch;
    }

}
