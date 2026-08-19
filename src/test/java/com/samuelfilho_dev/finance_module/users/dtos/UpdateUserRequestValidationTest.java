package com.samuelfilho_dev.finance_module.users.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidPayloadWithoutAddress() {
        var payload = new UpdateUserRequest("Samuel", "samuel@test.com", null);
        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectInvalidEmailAndNullName() {
        var payload = new UpdateUserRequest(null, "not-an-email", null);
        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectInvalidNestedAddress() {
        var address = new AddressRequest(" ", "10", null, "SP", "SAO", "123");
        var payload = new UpdateUserRequest("Samuel", "samuel@test.com", address);
        assertFalse(validator.validate(payload).isEmpty());
    }
}
