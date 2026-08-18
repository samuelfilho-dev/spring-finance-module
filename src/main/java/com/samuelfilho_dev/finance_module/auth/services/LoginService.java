package com.samuelfilho_dev.finance_module.auth.services;

import com.samuelfilho_dev.finance_module.auth.dtos.AuthGenericResponse;
import com.samuelfilho_dev.finance_module.auth.dtos.CreateLoginRequest;
import com.samuelfilho_dev.finance_module.auth.dtos.MfaRequest;

public interface LoginService {

    AuthGenericResponse login(CreateLoginRequest payload);

    AuthGenericResponse verifyMfaFactor(MfaRequest payload);

    void enableMfaFactor(MfaRequest payload);
}
