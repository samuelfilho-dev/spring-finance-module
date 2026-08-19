package com.samuelfilho_dev.finance_module.users.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidPayload() {
        var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", validAddress());
        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectInvalidEmailAndBlankName() {
        var payload = new CreateUserRequest(null, "not-an-email", "secret", null);
        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectInvalidNestedAddress() {
        var address = new AddressRequest(" ", "10", null, "SP", "SAO", "123");
        var payload = new CreateUserRequest("Samuel", "samuel@test.com", "secret", address);
        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void addressRequest_shouldRejectInvalidStateAndPostalCode() {
        var address = new AddressRequest("Rua A", "10", null, "São Paulo", "SAO", "123");
        assertFalse(validator.validate(address).isEmpty());
    }

    @Test
    void addressRequest_shouldAcceptValidPayload() {
        assertTrue(validator.validate(validAddress()).isEmpty());
    }

    private static AddressRequest validAddress() {
        return new AddressRequest("Rua A", "10", null, "São Paulo", "SP", "01310100");
    }
}
