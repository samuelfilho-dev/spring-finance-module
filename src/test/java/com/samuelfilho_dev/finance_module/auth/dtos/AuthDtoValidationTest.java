package com.samuelfilho_dev.finance_module.auth.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void loginRequest_shouldRequireEmailAndPassword() {
        assertFalse(validator.validate(new CreateLoginRequest("", "")).isEmpty());
        assertTrue(validator.validate(new CreateLoginRequest("user@test.com", "secret")).isEmpty());
    }

    @Test
    void mfaRequest_shouldRequireSixDigitCode() {
        assertFalse(validator.validate(new MfaRequest("user@test.com", "12ab")).isEmpty());
        assertTrue(validator.validate(new MfaRequest("user@test.com", "123456")).isEmpty());
    }

    @Test
    void mfaRequest_shouldRejectBlankEmail() {
        assertFalse(validator.validate(new MfaRequest(" ", "123456")).isEmpty());
    }

    @Test
    void resetMfaRequest_shouldRequireValidEmail() {
        assertFalse(validator.validate(new ResetMfaRequest("invalid")).isEmpty());
        assertTrue(validator.validate(new ResetMfaRequest("user@test.com")).isEmpty());
    }
}
