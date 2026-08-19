package com.samuelfilho_dev.finance_module.account.services;

import com.samuelfilho_dev.finance_module.account.dtos.BankAccountResponse;
import com.samuelfilho_dev.finance_module.account.dtos.CreateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.dtos.UpdateBankAccountRequest;
import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.account.mappers.BankAccountMapper;
import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.account.services.impl.BankAccountServiceImpl;
import com.samuelfilho_dev.finance_module.auth.entities.AuthenticatedUser;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String OTHER_USER_ID = new ObjectId().toHexString();
    private static final String ACCOUNT_ID = new ObjectId().toHexString();

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private BankAccountServiceImpl bankAccountService;

    @BeforeEach
    void setUp() {
        authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class CreateBankAccount {

        @Test
        void shouldCreateActiveAccountLinkedToAuthenticatedUser() {
            var payload = new CreateBankAccountRequest("Nubank", "0001", "12345-6", new BigDecimal("150.50"));
            var saved = ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE);
            var response = responseOf(saved);

            when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(saved);
            when(bankAccountMapper.toResponse(saved)).thenReturn(response);

            var result = bankAccountService.createBankAccount(payload);

            var captor = ArgumentCaptor.forClass(BankAccount.class);
            verify(bankAccountRepository).save(captor.capture());
            var toSave = captor.getValue();

            assertEquals("BANCO_NUBANK", toSave.getBankName());
            assertEquals("0001", toSave.getAgency());
            assertEquals("12345-6", toSave.getAccountNumber());
            assertEquals(new BigDecimal("150.50"), toSave.getBalance());
            assertEquals(BankAccountStatus.ACTIVE, toSave.getStatus());
            assertEquals(USER_ID, toSave.getUserId().toHexString());
            assertEquals(response, result);
        }

        @Test
        void shouldDefaultBalanceToZeroWhenPayloadBalanceIsNull() {
            var payload = new CreateBankAccountRequest("Nubank", "0001", "12345-6", null);
            when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(bankAccountMapper.toResponse(any(BankAccount.class))).thenReturn(responseOf(ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE)));

            bankAccountService.createBankAccount(payload);

            var captor = ArgumentCaptor.forClass(BankAccount.class);
            verify(bankAccountRepository).save(captor.capture());
            assertEquals(BigDecimal.ZERO, captor.getValue().getBalance());
        }

        @ParameterizedTest
        @CsvSource({
                "Nubank, BANCO_NUBANK",
                "BANCO_ITAU, BANCO_ITAU",
                "Banco do Brasil, BANCO_DO_BRASIL",
                "'  inter  ', BANCO_INTER"
        })
        void shouldNormalizeBankName(String rawName, String expectedName) {
            var payload = new CreateBankAccountRequest(rawName, "0001", "12345-6", BigDecimal.TEN);
            when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(bankAccountMapper.toResponse(any(BankAccount.class)))
                    .thenReturn(responseOf(ownedAccount(ACCOUNT_ID, expectedName, BankAccountStatus.ACTIVE)));

            bankAccountService.createBankAccount(payload);

            var captor = ArgumentCaptor.forClass(BankAccount.class);
            verify(bankAccountRepository).save(captor.capture());
            assertEquals(expectedName, captor.getValue().getBankName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void shouldRejectBlankBankName(String bankName) {
            var payload = new CreateBankAccountRequest(bankName, "0001", "12345-6", BigDecimal.TEN);

            var exception = assertThrows(BusinessException.class, () -> bankAccountService.createBankAccount(payload));

            assertEquals("Nome do Banco é requerido", exception.getMessage());
            verify(bankAccountRepository, never()).save(any());
        }
    }

    @Nested
    class FindAllBankAccounts {

        @Test
        void shouldReturnOnlyActiveAccountsOwnedByAuthenticatedUser() {
            var ownedActive = ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE);
            var ownedInactive = ownedAccount(new ObjectId().toHexString(), "BANCO_ITAU", BankAccountStatus.INACTIVE);
            var otherUserActive = account(new ObjectId().toHexString(), "BANCO_BRADESCO", BankAccountStatus.ACTIVE, OTHER_USER_ID);
            var withoutOwner = account(new ObjectId().toHexString(), "BANCO_SANTANDER", BankAccountStatus.ACTIVE, null);
            var expected = List.of(responseOf(ownedActive));

            when(bankAccountRepository.findAll()).thenReturn(List.of(ownedActive, ownedInactive, otherUserActive, withoutOwner));
            when(bankAccountMapper.toResponseList(List.of(ownedActive))).thenReturn(expected);

            var result = bankAccountService.findAllBankAccounts();

            assertEquals(expected, result);
            verify(bankAccountMapper).toResponseList(List.of(ownedActive));
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoMatchingAccounts() {
            when(bankAccountRepository.findAll()).thenReturn(List.of());
            when(bankAccountMapper.toResponseList(anyList())).thenReturn(List.of());

            var result = bankAccountService.findAllBankAccounts();

            assertEquals(List.of(), result);
        }
    }

    @Nested
    class FindBankAccountById {

        @Test
        void shouldReturnAccountWhenOwnedByAuthenticatedUser() {
            var account = ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE);
            var response = responseOf(account);

            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(bankAccountMapper.toResponse(account)).thenReturn(response);

            var result = bankAccountService.findBankAccountById(ACCOUNT_ID);

            assertEquals(response, result);
        }

        @Test
        void shouldThrowNotFoundWhenAccountDoesNotExist() {
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            var exception = assertThrows(NotFoundException.class, () -> bankAccountService.findBankAccountById(ACCOUNT_ID));

            assertEquals("Conta Bancaria não encontrada", exception.getMessage());
            verify(bankAccountMapper, never()).toResponse(any());
        }

        @Test
        void shouldThrowForbiddenWhenAccountBelongsToAnotherUser() {
            var account = account(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE, OTHER_USER_ID);
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            var exception = assertThrows(ForbiddenException.class, () -> bankAccountService.findBankAccountById(ACCOUNT_ID));

            assertEquals("Você não tem permissão para acessar esta conta bancária", exception.getMessage());
        }
    }

    @Nested
    class UpdateBankAccount {

        @Test
        void shouldUpdateMutableFieldsAndKeepBalanceAndStatus() {
            var account = ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE);
            account.setBalance(new BigDecimal("80.00"));
            var payload = new UpdateBankAccountRequest("BANCO_ITAU", "4321", "99999-0");
            var response = new BankAccountResponse(ACCOUNT_ID, "BANCO_ITAU", "4321", "99999-0", "80.00");

            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(bankAccountRepository.save(account)).thenReturn(account);
            when(bankAccountMapper.toResponse(account)).thenReturn(response);

            var result = bankAccountService.updateBankAccount(ACCOUNT_ID, payload);

            assertEquals("BANCO_ITAU", account.getBankName());
            assertEquals("4321", account.getAgency());
            assertEquals("99999-0", account.getAccountNumber());
            assertEquals(new BigDecimal("80.00"), account.getBalance());
            assertEquals(BankAccountStatus.ACTIVE, account.getStatus());
            assertEquals(response, result);
            verify(bankAccountRepository).save(account);
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingMissingAccount() {
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () ->
                    bankAccountService.updateBankAccount(ACCOUNT_ID, new UpdateBankAccountRequest("BANCO_ITAU", "1", "2")));
            verify(bankAccountRepository, never()).save(any());
        }

        @Test
        void shouldThrowForbiddenWhenUpdatingAccountOfAnotherUser() {
            var account = account(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE, OTHER_USER_ID);
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThrows(ForbiddenException.class, () ->
                    bankAccountService.updateBankAccount(ACCOUNT_ID, new UpdateBankAccountRequest("BANCO_ITAU", "1", "2")));
            verify(bankAccountRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteBankAccount {

        @Test
        void shouldSoftDeleteByMarkingAccountInactive() {
            var account = ownedAccount(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE);
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(bankAccountRepository.save(account)).thenReturn(account);

            bankAccountService.deleteBankAccount(ACCOUNT_ID);

            assertEquals(BankAccountStatus.INACTIVE, account.getStatus());
            verify(bankAccountRepository).save(account);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingMissingAccount() {
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> bankAccountService.deleteBankAccount(ACCOUNT_ID));
            verify(bankAccountRepository, never()).save(any());
        }

        @Test
        void shouldThrowForbiddenWhenDeletingAccountOfAnotherUser() {
            var account = account(ACCOUNT_ID, "BANCO_NUBANK", BankAccountStatus.ACTIVE, OTHER_USER_ID);
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThrows(ForbiddenException.class, () -> bankAccountService.deleteBankAccount(ACCOUNT_ID));
            verify(bankAccountRepository, never()).save(any());
        }
    }

    private static void authenticate(String userId) {
        var principal = new AuthenticatedUser(userId, "user@test.com", "secret", List.of());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static BankAccount ownedAccount(String id, String bankName, BankAccountStatus status) {
        return account(id, bankName, status, USER_ID);
    }

    private static BankAccount account(String id, String bankName, BankAccountStatus status, String userId) {
        return BankAccount.builder()
                .id(id)
                .bankName(bankName)
                .agency("0001")
                .accountNumber("12345-6")
                .balance(BigDecimal.TEN)
                .status(status)
                .userId(userId == null ? null : new ObjectId(userId))
                .build();
    }

    private static BankAccountResponse responseOf(BankAccount account) {
        assertNotNull(account);
        return new BankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getAgency(),
                account.getAccountNumber(),
                account.getBalance().toPlainString()
        );
    }
}
