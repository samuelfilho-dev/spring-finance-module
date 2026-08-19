package com.samuelfilho_dev.finance_module.account.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBankAccountRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidPayload() {
        var payload = new CreateBankAccountRequest("Nubank", "0001", "12345-6", BigDecimal.TEN);

        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldAcceptNullBalance() {
        var payload = new CreateBankAccountRequest("Nubank", "0001", "12345-6", null);

        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectBlankBankName() {
        var payload = new CreateBankAccountRequest("  ", "0001", "12345-6", BigDecimal.TEN);

        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectBlankAgency() {
        var payload = new CreateBankAccountRequest("Nubank", "", "12345-6", BigDecimal.TEN);

        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectBlankAccountNumber() {
        var payload = new CreateBankAccountRequest("Nubank", "0001", " ", BigDecimal.TEN);

        assertFalse(validator.validate(payload).isEmpty());
    }
}
