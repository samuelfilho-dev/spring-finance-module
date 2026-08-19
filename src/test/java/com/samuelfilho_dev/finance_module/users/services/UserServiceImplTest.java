package com.samuelfilho_dev.finance_module.users.services;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthPreTokenResponse;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import com.samuelfilho_dev.finance_module.auth.services.MfaFactorService;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.NotFoundException;
import com.samuelfilho_dev.finance_module.support.TestSupport;
import com.samuelfilho_dev.finance_module.users.dtos.AddressRequest;
import com.samuelfilho_dev.finance_module.users.dtos.AddressResponse;
import com.samuelfilho_dev.finance_module.users.dtos.CreateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UpdateUserRequest;
import com.samuelfilho_dev.finance_module.users.dtos.UserResponse;
import com.samuelfilho_dev.finance_module.users.entities.Address;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.mappers.UserMapper;
import com.samuelfilho_dev.finance_module.users.repositories.AddressRepository;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.users.services.impl.UserServiceImpl;
import com.samuelfilho_dev.finance_module.utils.AESService;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String OTHER_USER_ID = new ObjectId().toHexString();

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private MfaFactorService mfaFactorService;
    @Mock
    private AESService aesService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        TestSupport.authenticate(USER_ID);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clearSecurityContext();
    }

    @Nested
    class CreateUser {

        @Test
        void shouldCreateUserAndReturnSetupPayload() {
            var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", null);
            when(userRepository.existsByEmail(payload.email())).thenReturn(false);
            when(mfaFactorService.generateSecret()).thenReturn("plain-secret");
            when(aesService.encrypt("plain-secret")).thenReturn("encrypted-secret");
            when(passwordEncoder.encode("secret")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                var user = invocation.getArgument(0, User.class);
                user.setId(USER_ID);
                return user;
            });
            when(mfaFactorService.buildOtpAuthUrl("samuel@test.com", "encrypted-secret")).thenReturn("otpauth://url");
            when(mfaFactorService.generateQrCodeImageBase64("samuel@test.com", "encrypted-secret")).thenReturn("qr");
            when(jwtService.generateSetupToken(any(User.class))).thenReturn("setup-token");

            var result = userService.createUser(payload);

            assertEquals(new AuthPreTokenResponse(
                    true,
                    "2FA é obrigatório. Escaneie o QR Code enviado",
                    "/api/v1/auth/mfa/enable",
                    "setup-token",
                    "qr",
                    "otpauth://url"
            ), result);
            verify(addressRepository, never()).save(any());
        }

        @Test
        void shouldPersistAddressWhenProvided() {
            var address = new AddressRequest("Rua A", "10", "Apto", "São Paulo", "SP", "01310100");
            var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", address);
            when(userRepository.existsByEmail(payload.email())).thenReturn(false);
            when(mfaFactorService.generateSecret()).thenReturn("plain-secret");
            when(aesService.encrypt("plain-secret")).thenReturn("encrypted-secret");
            when(passwordEncoder.encode("secret")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                var user = invocation.getArgument(0, User.class);
                user.setId(USER_ID);
                return user;
            });
            when(mfaFactorService.buildOtpAuthUrl(any(), any())).thenReturn("otpauth://url");
            when(mfaFactorService.generateQrCodeImageBase64(any(), any())).thenReturn("qr");
            when(jwtService.generateSetupToken(any(User.class))).thenReturn("setup-token");

            userService.createUser(payload);

            var captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(captor.capture());
            assertEquals("Rua A", captor.getValue().getStreet());
            assertEquals(USER_ID, captor.getValue().getUserId().toHexString());
        }

        @Test
        void shouldRejectDuplicatedEmail() {
            var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", null);
            when(userRepository.existsByEmail(payload.email())).thenReturn(true);

            var exception = assertThrows(BusinessException.class, () -> userService.createUser(payload));

            assertEquals("Email já cadastrado", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class CreateAdminUser {

        @Test
        void shouldCreateAdminWithRoleAdmin() {
            var payload = new CreateUserRequest("Admin", "admin@test.com", "secret", null);
            var saved = User.builder().id(USER_ID).email(payload.email()).role("ROLE_ADMIN").build();
            var response = new UserResponse(USER_ID, "Admin", payload.email(), null, null);

            when(userRepository.existsByEmail(payload.email())).thenReturn(false);
            when(mfaFactorService.generateSecret()).thenReturn("plain-secret");
            when(aesService.encrypt("plain-secret")).thenReturn("encrypted-secret");
            when(passwordEncoder.encode("secret")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(saved);
            when(userMapper.toResponse(saved)).thenReturn(response);

            var result = userService.createAdminUser(payload);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("ROLE_ADMIN", captor.getValue().getRole());
            assertEquals(response, result);
        }

        @Test
        void shouldRejectDuplicatedAdminEmail() {
            var payload = new CreateUserRequest("Admin", "admin@test.com", "secret", null);
            when(userRepository.existsByEmail(payload.email())).thenReturn(true);

            assertThrows(BusinessException.class, () -> userService.createAdminUser(payload));
        }
    }

    @Nested
    class Queries {

        @Test
        void findAllUsers_shouldAggregateAndMap() {
            var user = User.builder().id(USER_ID).email("samuel@test.com").build();
            var response = new UserResponse(USER_ID, "Samuel", "samuel@test.com", null, null);
            stubAggregate(List.of(user));
            when(userMapper.toResponseList(List.of(user))).thenReturn(List.of(response));

            var result = userService.findAllUsers();

            assertEquals(List.of(response), result);
        }

        @Test
        void findAllUsers_shouldReturnEmptyList() {
            stubAggregate(List.of());
            when(userMapper.toResponseList(List.of())).thenReturn(List.of());

            assertEquals(List.of(), userService.findAllUsers());
        }

        @Test
        void findUserById_shouldReturnOwnUser() {
            var user = User.builder().id(USER_ID).email("samuel@test.com").build();
            var response = new UserResponse(USER_ID, "Samuel", "samuel@test.com", null, null);
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            var result = userService.findUserById(USER_ID);

            assertEquals(response, result);
        }

        @Test
        void findUserById_shouldHideOtherUsersAsNotFound() {
            var exception = assertThrows(NotFoundException.class, () -> userService.findUserById(OTHER_USER_ID));
            assertEquals("Usuário não encontrado", exception.getMessage());
        }

        @Test
        void findUserById_shouldThrowWhenAggregationIsEmpty() {
            stubAggregate(List.of());
            assertThrows(NotFoundException.class, () -> userService.findUserById(USER_ID));
        }

        @Test
        void findUserByEmail_shouldMapFoundUser() {
            var user = User.builder().id(USER_ID).email("samuel@test.com").build();
            var response = new UserResponse(USER_ID, "Samuel", "samuel@test.com", null, null);
            when(userRepository.findUserByEmail("samuel@test.com")).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            assertEquals(response, userService.findUserByEmail("samuel@test.com"));
        }

        @Test
        void findUserByEmail_shouldThrowWhenMissing() {
            when(userRepository.findUserByEmail("missing@test.com")).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> userService.findUserByEmail("missing@test.com"));
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void updateUserById_shouldUpdateUserAndExistingAddress() {
            var user = User.builder().id(USER_ID).name("Old").email("old@test.com").build();
            var address = Address.builder().id("addr-1").userId(new ObjectId(USER_ID)).build();
            var payload = new UpdateUserRequest(
                    "New",
                    "new@test.com",
                    new AddressRequest("Rua B", "20", null, "Campinas", "SP", "13000000")
            );
            var response = new UserResponse(USER_ID, "New", "new@test.com", new AddressResponse("addr-1", "Rua B", "20", null, "Campinas", "SP", "13000000"), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(addressRepository.findByUserId(new ObjectId(USER_ID))).thenReturn(Optional.of(address));
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            var result = userService.updateUserById(USER_ID, payload);

            verify(userRepository).save(user);
            verify(addressRepository).save(address);
            assertEquals("New", user.getName());
            assertEquals("Rua B", address.getStreet());
            assertEquals(response, result);
        }

        @Test
        void updateUserById_shouldCreateAddressWhenMissing() {
            var user = User.builder().id(USER_ID).name("Old").email("old@test.com").build();
            var payload = new UpdateUserRequest(
                    "New",
                    "new@test.com",
                    new AddressRequest("Rua B", "20", null, "Campinas", "SP", "13000000")
            );
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(addressRepository.findByUserId(any())).thenReturn(Optional.empty());
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(new UserResponse(USER_ID, "New", "new@test.com", null, null));

            userService.updateUserById(USER_ID, payload);

            verify(addressRepository).save(any(Address.class));
        }

        @Test
        void updateUserById_shouldSkipAddressWhenPayloadHasNone() {
            var user = User.builder().id(USER_ID).name("Old").email("old@test.com").build();
            var payload = new UpdateUserRequest("New", "new@test.com", null);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(new UserResponse(USER_ID, "New", "new@test.com", null, null));

            userService.updateUserById(USER_ID, payload);

            verify(addressRepository, never()).save(any());
        }

        @Test
        void updateUserById_shouldThrowWhenUserDoesNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () ->
                    userService.updateUserById(USER_ID, new UpdateUserRequest("New", "new@test.com", null)));
        }

        @Test
        void updateUserById_shouldHideOtherUsersAsNotFoundAfterSave() {
            var user = User.builder().id(OTHER_USER_ID).name("Old").email("old@test.com").build();
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user));

            assertThrows(NotFoundException.class, () ->
                    userService.updateUserById(OTHER_USER_ID, new UpdateUserRequest("New", "new@test.com", null)));
            verify(userRepository).save(user);
        }

        @Test
        void deleteUserById_shouldDeleteUserAndAddress() {
            var user = User.builder().id(USER_ID).email("samuel@test.com").build();
            var response = new UserResponse(
                    USER_ID,
                    "Samuel",
                    "samuel@test.com",
                    new AddressResponse("addr-1", "Rua A", "10", null, "SP", "SP", "01310100"),
                    null
            );
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            userService.deleteUserById(USER_ID);

            verify(addressRepository).deleteById("addr-1");
            verify(userRepository).deleteById(USER_ID);
        }

        @Test
        void deleteUserById_shouldSkipAddressDeleteWhenUserHasNone() {
            var user = User.builder().id(USER_ID).email("samuel@test.com").build();
            stubAggregate(List.of(user));
            when(userMapper.toResponse(user)).thenReturn(new UserResponse(USER_ID, "Samuel", "samuel@test.com", null, null));

            userService.deleteUserById(USER_ID);

            verify(addressRepository, never()).deleteById(any());
            verify(userRepository).deleteById(USER_ID);
        }
    }

    @SuppressWarnings("unchecked")
    private void stubAggregate(List<User> users) {
        AggregationResults<User> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(users);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq(User.class), eq(User.class))).thenReturn(results);
    }
}
