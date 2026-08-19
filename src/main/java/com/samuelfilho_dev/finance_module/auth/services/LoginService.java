package com.samuelfilho_dev.finance_module.auth.services;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthResponse;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.ResetMfaRequest;

public interface LoginService {

    AuthResponse login(CreateLoginRequest payload);

    AuthResponse verifyMfaFactor(MfaRequest payload);

    void enableMfaFactor(MfaRequest payload);

    AuthResponse resetMfaFactor(ResetMfaRequest payload);
}
