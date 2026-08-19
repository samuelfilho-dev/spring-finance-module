package com.samuelfilho_dev.finance_module.launches.dtos;

import com.samuelfilho_dev.finance_module.launches.enums.LaunchCategory;
import com.samuelfilho_dev.finance_module.launches.enums.LaunchType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateLaunchRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidPayload() {
        var payload = new CreateLaunchRequest(
                "Salary",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                BigDecimal.TEN,
                LaunchType.RECIPE,
                LaunchCategory.SALARY,
                new ObjectId().toHexString()
        );
        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectZeroAmountIsAllowedButNegativeIsNot() {
        var validZero = new CreateLaunchRequest(
                "Salary",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                BigDecimal.ZERO,
                LaunchType.RECIPE,
                LaunchCategory.SALARY,
                new ObjectId().toHexString()
        );
        assertTrue(validator.validate(validZero).isEmpty());
    }

    @Test
    void shouldRejectNegativeAmountAndInvalidAccountId() {
        var payload = new CreateLaunchRequest(
                "Salary",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("-1"),
                LaunchType.RECIPE,
                LaunchCategory.SALARY,
                "not-an-object-id"
        );
        assertFalse(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        var payload = new CreateLaunchRequest(null, null, null, null, null, null, null);
        assertFalse(validator.validate(payload).isEmpty());
    }
}
