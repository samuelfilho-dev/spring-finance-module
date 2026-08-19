package com.samuelfilho_dev.finance_module.launches.services;

import com.samuelfilho_dev.finance_module.account.entities.BankAccount;
import com.samuelfilho_dev.finance_module.account.enums.BankAccountStatus;
import com.samuelfilho_dev.finance_module.account.repositories.BankAccountRepository;
import com.samuelfilho_dev.finance_module.exceptions.ForbiddenException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.launches.dtos.CreateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.dtos.LaunchResponse;
import com.samuelfilho_dev.finance_module.launches.dtos.UpdateLaunchRequest;
import com.samuelfilho_dev.finance_module.launches.entities.Launch;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import com.samuelfilho_dev.finance_module.launches.mappers.LaunchMapper;
import com.samuelfilho_dev.finance_module.launches.respositories.LaunchRepository;
import com.samuelfilho_dev.finance_module.launches.services.impl.LaunchServiceImpl;
import com.samuelfilho_dev.finance_module.support.TestSupport;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchServiceImplTest {

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String OTHER_USER_ID = new ObjectId().toHexString();
    private static final String ACCOUNT_ID = new ObjectId().toHexString();
    private static final String LAUNCH_ID = new ObjectId().toHexString();

    @Mock
    private LaunchRepository launchRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private LaunchMapper launchMapper;

    @InjectMocks
    private LaunchServiceImpl launchService;

    @BeforeEach
    void setUp() {
        TestSupport.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearSecurityContext();
    }

    @Nested
    class Find {

        @Test
        void findAllLaunches_shouldReturnOnlyOwnedLaunches() {
            var owned = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10"));
            var other = launch(new ObjectId().toHexString(), OTHER_USER_ID, LaunchType.EXPENSE, new BigDecimal("5"));
            var response = responseOf(owned);
            when(launchRepository.findAll()).thenReturn(List.of(owned, other));
            when(launchMapper.toResponseList(List.of(owned))).thenReturn(List.of(response));

            var result = launchService.findAllLaunches();

            assertEquals(List.of(response), result);
        }

        @Test
        void findLaunchById_shouldReturnOwnedLaunch() {
            var owned = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10"));
            var response = responseOf(owned);
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(owned));
            when(launchMapper.toResponse(owned)).thenReturn(response);

            assertEquals(response, launchService.findLaunchById(LAUNCH_ID));
        }

        @Test
        void findLaunchById_shouldThrowWhenMissing() {
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> launchService.findLaunchById(LAUNCH_ID));
        }

        @Test
        void findLaunchById_shouldHideOtherUsersLaunchesAsNotFound() {
            var other = launch(LAUNCH_ID, OTHER_USER_ID, LaunchType.RECIPE, new BigDecimal("10"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(other));
            assertThrows(NotFoundException.class, () -> launchService.findLaunchById(LAUNCH_ID));
        }

        @Test
        void findAllLaunches_shouldReturnEmptyListWhenUserHasNoLaunches() {
            when(launchRepository.findAll()).thenReturn(List.of());
            when(launchMapper.toResponseList(List.of())).thenReturn(List.of());

            assertEquals(List.of(), launchService.findAllLaunches());
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateRecipeAndIncreaseBalance() {
            var payload = new CreateLaunchRequest(
                    "Salary",
                    "desc",
                    Instant.parse("2026-01-15T00:00:00Z"),
                    new BigDecimal("100.00"),
                    LaunchType.RECIPE,
                    LaunchCategory.SALARY,
                    ACCOUNT_ID
            );
            var account = ownedAccount(new BigDecimal("50.00"));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(any(Launch.class))).thenReturn(responseOf(launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, payload.amount())));

            launchService.createLaunch(payload);

            var launchCaptor = ArgumentCaptor.forClass(Launch.class);
            verify(launchRepository).save(launchCaptor.capture());
            assertEquals(USER_ID, launchCaptor.getValue().getUserId().toHexString());
            assertEquals(ACCOUNT_ID, launchCaptor.getValue().getBankAccountId().toHexString());
            assertEquals(new BigDecimal("150.00"), account.getBalance());
            verify(bankAccountRepository).save(account);
        }

        @Test
        void shouldCreateExpenseAndDecreaseBalance() {
            var payload = new CreateLaunchRequest(
                    "Food",
                    null,
                    Instant.parse("2026-01-15T00:00:00Z"),
                    new BigDecimal("30.00"),
                    LaunchType.EXPENSE,
                    LaunchCategory.FOOD,
                    ACCOUNT_ID
            );
            var account = ownedAccount(new BigDecimal("50.00"));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(any(Launch.class))).thenReturn(responseOf(launch(LAUNCH_ID, USER_ID, LaunchType.EXPENSE, payload.amount())));

            launchService.createLaunch(payload);

            assertEquals(new BigDecimal("20.00"), account.getBalance());
        }

        @Test
        void shouldThrowWhenBankAccountIsMissing() {
            var payload = new CreateLaunchRequest("Salary", null, Instant.now(), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY, ACCOUNT_ID);
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> launchService.createLaunch(payload));
            verify(launchRepository, never()).save(any());
        }

        @Test
        void shouldRejectLaunchOnSomeoneElsesAccount() {
            var payload = new CreateLaunchRequest("Salary", null, Instant.now(), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY, ACCOUNT_ID);
            var account = account(OTHER_USER_ID, new BigDecimal("50.00"));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThrows(ForbiddenException.class, () -> launchService.createLaunch(payload));
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void updateLaunch_shouldRecalculateBalanceWhenAmountChanges() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10.00"));
            var account = ownedAccount(new BigDecimal("110.00"));
            var payload = new UpdateLaunchRequest("New", "d", Instant.parse("2026-01-16T00:00:00Z"), new BigDecimal("25.00"), LaunchType.RECIPE, LaunchCategory.BONUS);
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(launch)).thenReturn(responseOf(launch));

            launchService.updateLaunch(LAUNCH_ID, payload);

            assertEquals("New", launch.getTitle());
            assertEquals(new BigDecimal("125.00"), account.getBalance());
            verify(bankAccountRepository).save(account);
        }

        @Test
        void updateLaunch_shouldKeepBalanceWhenAmountAndTypeStayTheSame() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10.00"));
            var account = ownedAccount(new BigDecimal("110.00"));
            var payload = new UpdateLaunchRequest("New", "d", Instant.parse("2026-01-16T00:00:00Z"), new BigDecimal("10.00"), LaunchType.RECIPE, LaunchCategory.SALARY);
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(launch)).thenReturn(responseOf(launch));

            launchService.updateLaunch(LAUNCH_ID, payload);

            assertEquals(new BigDecimal("110.00"), account.getBalance());
            verify(bankAccountRepository, never()).save(account);
        }

        @Test
        void updateLaunch_shouldRecalculateBalanceWhenTypeChanges() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10.00"));
            var account = ownedAccount(new BigDecimal("110.00"));
            var payload = new UpdateLaunchRequest("New", "d", Instant.parse("2026-01-16T00:00:00Z"), new BigDecimal("10.00"), LaunchType.EXPENSE, LaunchCategory.FOOD);
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(launch)).thenReturn(responseOf(launch));

            launchService.updateLaunch(LAUNCH_ID, payload);

            assertEquals(new BigDecimal("90.00"), account.getBalance());
            verify(bankAccountRepository).save(account);
        }

        @Test
        void updateLaunch_shouldKeepOldAmountAndTypeWhenPayloadOmitsThem() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10.00"));
            var account = ownedAccount(new BigDecimal("110.00"));
            var payload = new UpdateLaunchRequest("New", "d", Instant.parse("2026-01-16T00:00:00Z"), null, null, LaunchCategory.SALARY);
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(launchMapper.toResponse(launch)).thenReturn(responseOf(launch));

            launchService.updateLaunch(LAUNCH_ID, payload);

            assertEquals(new BigDecimal("110.00"), account.getBalance());
            verify(bankAccountRepository, never()).save(account);
        }

        @Test
        void updateLaunch_shouldThrowWhenBankAccountIsMissing() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("10.00"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> launchService.updateLaunch(
                    LAUNCH_ID,
                    new UpdateLaunchRequest("New", "d", Instant.now(), new BigDecimal("25.00"), LaunchType.RECIPE, LaunchCategory.BONUS)
            ));
            verify(launchRepository, never()).save(any());
        }

        @Test
        void updateLaunch_shouldThrowWhenLaunchDoesNotExist() {
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> launchService.updateLaunch(
                    LAUNCH_ID,
                    new UpdateLaunchRequest("New", "d", Instant.now(), BigDecimal.TEN, LaunchType.RECIPE, LaunchCategory.SALARY)
            ));
        }

        @Test
        void deleteLaunch_shouldReverseRecipeBalanceAndDelete() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.RECIPE, new BigDecimal("20.00"));
            var account = ownedAccount(new BigDecimal("80.00"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            launchService.deleteLaunch(LAUNCH_ID);

            assertEquals(new BigDecimal("60.00"), account.getBalance());
            verify(launchRepository).delete(launch);
        }

        @Test
        void deleteLaunch_shouldThrowWhenLaunchBelongsToAnotherUser() {
            var other = launch(LAUNCH_ID, OTHER_USER_ID, LaunchType.EXPENSE, new BigDecimal("20.00"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(other));

            assertThrows(NotFoundException.class, () -> launchService.deleteLaunch(LAUNCH_ID));
            verify(launchRepository, never()).delete(any());
        }

        @Test
        void deleteLaunch_shouldReverseBalanceAndDelete() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.EXPENSE, new BigDecimal("20.00"));
            var account = ownedAccount(new BigDecimal("80.00"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            launchService.deleteLaunch(LAUNCH_ID);

            assertEquals(new BigDecimal("100.00"), account.getBalance());
            verify(bankAccountRepository).save(account);
            verify(launchRepository).delete(launch);
        }

        @Test
        void deleteLaunch_shouldThrowWhenBankAccountIsMissing() {
            var launch = launch(LAUNCH_ID, USER_ID, LaunchType.EXPENSE, new BigDecimal("20.00"));
            when(launchRepository.findById(LAUNCH_ID)).thenReturn(Optional.of(launch));
            when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> launchService.deleteLaunch(LAUNCH_ID));
            verify(launchRepository, never()).delete(any());
        }
    }

    private static Launch launch(String id, String userId, LaunchType type, BigDecimal amount) {
        return Launch.builder()
                .id(id)
                .title("Launch")
                .amount(amount)
                .type(type)
                .category(LaunchCategory.OTHER)
                .launchDate(Instant.parse("2026-01-01T00:00:00Z"))
                .userId(new ObjectId(userId))
                .bankAccountId(new ObjectId(ACCOUNT_ID))
                .build();
    }

    private static BankAccount ownedAccount(BigDecimal balance) {
        return account(USER_ID, balance);
    }

    private static BankAccount account(String userId, BigDecimal balance) {
        return BankAccount.builder()
                .id(ACCOUNT_ID)
                .bankName("BANCO_NUBANK")
                .agency("0001")
                .accountNumber("123")
                .balance(balance)
                .status(BankAccountStatus.ACTIVE)
                .userId(new ObjectId(userId))
                .build();
    }

    private static LaunchResponse responseOf(Launch launch) {
        return new LaunchResponse(
                launch.getId(),
                launch.getTitle(),
                launch.getDescription(),
                launch.getLaunchDate(),
                launch.getAmount(),
                launch.getType(),
                launch.getCategory()
        );
    }
}
