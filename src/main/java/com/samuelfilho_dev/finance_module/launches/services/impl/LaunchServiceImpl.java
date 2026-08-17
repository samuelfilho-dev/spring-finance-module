package com.samuelfilho_dev.finance_module.launches.services.impl;

import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchServiceImpl implements LaunchService {
    private final LaunchRepository launchRepository;
    private final BankAccountRepository bankAccountRepository;

    private final LaunchMapper launchMapper;
    private final UserRepository userRepository;

    @Override
    public List<LaunchResponse> findAllLaunches() {
        var launches = launchRepository.findAll();

        log.info("Foi listado os lançamentos");
        return launchMapper.toResponseList(launches);
    }

    @Override
    public LaunchResponse findLaunchById(String id) {
        var launch = this.launchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));

        log.info("Foi encontrado lançamento com ID: {}", launch.getId());
        return launchMapper.toResponse(launch);
    }

    @Override
    public LaunchResponse createLaunch(CreateLaunchRequest payload) {
        userRepository.findById(payload.userId())
                .orElseThrow(() -> new NotFoundException("Usuario não encontrado"));

        var bankAccount = bankAccountRepository.findById(payload.bankAccountId())
                .orElseThrow(() -> new NotFoundException("Conta Bancaria não encontrada"));

        var launch = Launch.builder()
                .title(payload.title())
                .description(payload.description())
                .launchDate(payload.launchDate())
                .amount(payload.amount())
                .type(payload.type())
                .userId(new ObjectId(payload.userId()))
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
        var launch = this.launchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));

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
        var launch = this.launchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado"));

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

}
