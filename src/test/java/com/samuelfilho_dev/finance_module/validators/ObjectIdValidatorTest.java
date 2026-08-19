package com.samuelfilho_dev.finance_module.validators;

import com.samuelfilho_dev.finance_module.validators.impl.ObjectIdValidator;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectIdValidatorTest {

    private final ObjectIdValidator validator = new ObjectIdValidator();

    @ParameterizedTest
    @NullAndEmptySource
    void shouldAcceptNullOrEmptyValues(String value) {
        assertTrue(validator.isValid(value, null));
    }

    @Test
    void shouldAcceptValidObjectId() {
        assertTrue(validator.isValid(new ObjectId().toHexString(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "665f1c2e8f1a2b3c4d5e6f7g"})
    void shouldRejectInvalidObjectIds(String value) {
        assertFalse(validator.isValid(value, null));
    }
}
