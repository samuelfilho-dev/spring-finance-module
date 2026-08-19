package com.samuelfilho_dev.finance_module.auth.services;

import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.ResetMfaRequest;
import com.samuelfilho_dev.finance_module.auth.services.impl.LoginServiceImpl;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.UnauthorizedException;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.utils.AESService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private MfaFactorService mfaFactorService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AESService aesService;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Nested
    class Login {

        @Test
        void shouldReturnSetupPayloadWhenMfaIsNotActivated() {
            var payload = new CreateLoginRequest("user@test.com", "secret");
            var user = User.builder().email(payload.email()).isMfaActivated(false).mfaSecret("encrypted").build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(jwtService.generateSetupToken(user)).thenReturn("setup-token");
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.generateQrCodeImageBase64(payload.email(), "plain")).thenReturn("qr");
            when(mfaFactorService.buildOtpAuthUrl(payload.email(), "plain")).thenReturn("otpauth://url");

            var result = loginService.login(payload);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userRepository, never()).save(any());
            assertEquals("2FA obrigatório e ainda não configurado.", result.message());
            assertEquals("/api/v1/auth/mfa/setup", result.path());
            assertEquals("setup-token", result.token());
            assertEquals("qr", result.qrCodeBase64());
        }

        @Test
        void shouldGenerateSecretWhenUserHasNone() {
            var payload = new CreateLoginRequest("user@test.com", "secret");
            var user = User.builder().email(payload.email()).isMfaActivated(false).mfaSecret(null).build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(jwtService.generateSetupToken(user)).thenReturn("setup-token");
            when(mfaFactorService.generateSecret()).thenReturn("plain");
            when(aesService.encrypt("plain")).thenReturn("encrypted");
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.generateQrCodeImageBase64(payload.email(), "plain")).thenReturn("qr");
            when(mfaFactorService.buildOtpAuthUrl(payload.email(), "plain")).thenReturn("otpauth://url");

            loginService.login(payload);

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("encrypted", captor.getValue().getMfaSecret());
        }

        @Test
        void shouldReturnPreAuthTokenWhenMfaIsActivated() {
            var payload = new CreateLoginRequest("user@test.com", "secret");
            var user = User.builder().email(payload.email()).isMfaActivated(true).build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(jwtService.generatePreAuthToken(user)).thenReturn("pre-token");

            var result = loginService.login(payload);

            assertEquals("Credenciais válidas. Informe o código do seu app autenticador", result.message());
            assertEquals("/api/v1/auth/mfa/verify", result.path());
            assertEquals("pre-token", result.token());
        }

        @Test
        void shouldPropagateBadCredentials() {
            var payload = new CreateLoginRequest("user@test.com", "wrong");
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

            assertThrows(BadCredentialsException.class, () -> loginService.login(payload));
        }

        @Test
        void shouldThrowWhenUserIsMissingAfterAuthentication() {
            var payload = new CreateLoginRequest("user@test.com", "secret");
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.empty());

            var exception = assertThrows(BusinessException.class, () -> loginService.login(payload));
            assertEquals("Email ou senha incorreto", exception.getMessage());
        }
    }

    @Nested
    class VerifyAndEnable {

        @Test
        void verifyMfaFactor_shouldReturnAccessTokenWhenCodeIsValid() {
            var payload = new MfaRequest("user@test.com", "123456");
            var user = User.builder().email(payload.email()).mfaSecret("encrypted").build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.verifyCode("plain", "123456")).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn("access-token");

            var result = loginService.verifyMfaFactor(payload);

            assertEquals("MFA verificado com sucesso", result.message());
            assertEquals("access-token", result.token());
        }

        @Test
        void verifyMfaFactor_shouldRejectInvalidCode() {
            var payload = new MfaRequest("user@test.com", "000000");
            var user = User.builder().email(payload.email()).mfaSecret("encrypted").build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.verifyCode("plain", "000000")).thenReturn(false);

            var exception = assertThrows(UnauthorizedException.class, () -> loginService.verifyMfaFactor(payload));
            assertEquals("Falha na validação do MFA", exception.getMessage());
        }

        @Test
        void verifyMfaFactor_shouldThrowWhenUserIsMissing() {
            var payload = new MfaRequest("missing@test.com", "123456");
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.empty());

            var exception = assertThrows(BusinessException.class, () -> loginService.verifyMfaFactor(payload));
            assertEquals("Email ou senha incorreto", exception.getMessage());
        }

        @Test
        void enableMfaFactor_shouldActivateWhenCodeIsValid() {
            var payload = new MfaRequest("user@test.com", "123456");
            var user = User.builder().email(payload.email()).mfaSecret("encrypted").isMfaActivated(false).build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.verifyCode("plain", "123456")).thenReturn(true);

            loginService.enableMfaFactor(payload);

            verify(userRepository).save(user);
            assertEquals(true, user.getIsMfaActivated());
        }

        @Test
        void enableMfaFactor_shouldRejectWhenAlreadyEnabled() {
            var payload = new MfaRequest("user@test.com", "123456");
            var user = User.builder().email(payload.email()).isMfaActivated(true).build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));

            var exception = assertThrows(BusinessException.class, () -> loginService.enableMfaFactor(payload));
            assertEquals("2FA já está ativado para este usuário", exception.getMessage());
        }

        @Test
        void enableMfaFactor_shouldRejectInvalidCode() {
            var payload = new MfaRequest("user@test.com", "000000");
            var user = User.builder().email(payload.email()).mfaSecret("encrypted").isMfaActivated(false).build();
            when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
            when(aesService.decrypt("encrypted")).thenReturn("plain");
            when(mfaFactorService.verifyCode("plain", "000000")).thenReturn(false);

            var exception = assertThrows(BusinessException.class, () -> loginService.enableMfaFactor(payload));
            assertEquals("Código 2FA inválido", exception.getMessage());
        }
    }

    @Test
    void resetMfaFactor_shouldRotateSecretAndDisableMfa() {
        var payload = new ResetMfaRequest("user@test.com");
        var user = User.builder().email(payload.email()).isMfaActivated(true).mfaSecret("old").build();
        when(userRepository.findUserByEmail(payload.email())).thenReturn(Optional.of(user));
        when(mfaFactorService.generateSecret()).thenReturn("new-plain");
        when(aesService.encrypt("new-plain")).thenReturn("new-encrypted");

        var result = loginService.resetMfaFactor(payload);

        assertFalse(user.getIsMfaActivated());
        assertEquals("new-encrypted", user.getMfaSecret());
        assertEquals("2FA resetado com sucesso", result.message());
        verify(userRepository).save(user);
    }
}
