package com.samuelfilho_dev.finance_module.auth.services.impl;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthGenericResponse;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.services.JwtService;
import com.samuelfilho_dev.finance_module.auth.services.LoginService;
import com.samuelfilho_dev.finance_module.auth.services.MfaFactorService;
import com.samuelfilho_dev.finance_module.exceptions.BusinessException;
import com.samuelfilho_dev.finance_module.exceptions.UnauthorizedException;
import com.samuelfilho_dev.finance_module.users.entities.User;
import com.samuelfilho_dev.finance_module.users.repositories.UserRepository;
import com.samuelfilho_dev.finance_module.utils.AESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;
    private final MfaFactorService mfaFactorService;
    private final JwtService jwtService;
    private final AESService aesService;

    @Override
    public AuthGenericResponse login(CreateLoginRequest payload) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(payload.email(), payload.password())
        );

        var user = this.findUserByEmailHandler(payload.email());

        if (!user.getIsMfaActivated()) {
            log.error("Usuario {} não tem 2FA ativo", payload.email());
            throw new UnauthorizedException("Usuario sem 2FA ativo");
        }

        var preToken = this.jwtService.generatePreAuthToken(user);

        return new AuthGenericResponse(
                true,
                "Credenciais válidas. Informe o código do seu app autenticador",
                "/api/v1/auth/mfa/verify",
                preToken
        );
    }

    @Override
    public AuthGenericResponse verifyMfaFactor(MfaRequest payload) {
        var user = findUserByEmailHandler(payload.email());

        var secret = aesService.decrypt(user.getMfaSecret());
        var valid = this.mfaFactorService.verifyCode(secret, payload.code());

        if (!valid) {
            throw new UnauthorizedException("Falha na validação do MFA");
        }

        var token = this.jwtService.generateAccessToken(user);

        return new AuthGenericResponse(
                true,
                "MFA verificado com sucesso",
                null,
                token
        );
    }

    @Override
    public void enableMfaFactor(MfaRequest payload) {
        var user = findUserByEmailHandler(payload.email());

        if (user.getIsMfaActivated()) {
            throw new BusinessException("2FA já está ativado para este usuário");
        }

        var secret = aesService.decrypt(user.getMfaSecret());
        var valid = this.mfaFactorService.verifyCode(secret, payload.code());

        if (!valid) {
            throw new BusinessException("Código 2FA inválido");
        }

        user.setIsMfaActivated(true);
        userRepository.save(user);
    }

    private User findUserByEmailHandler(String email) {
        return userRepository.findUserByEmail(email).orElseThrow(
                () -> {
                    log.error("Usuário não encontrado com esse email {}", email);
                    return new BusinessException("Email ou código incorreto");
                }
        );
    }

}
