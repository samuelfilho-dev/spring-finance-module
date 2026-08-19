package com.samuelfilho_dev.finance_module.launches.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOfxParserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidAccountId() {
        var file = new MockMultipartFile("file", "statement.ofx", "application/x-ofx", new byte[]{1});
        var payload = new CreateOfxParserRequest(new ObjectId().toHexString(), file);
        assertTrue(validator.validate(payload).isEmpty());
    }

    @Test
    void shouldRejectBlankAndInvalidAccountId() {
        var file = new MockMultipartFile("file", "statement.ofx", "application/x-ofx", new byte[]{1});
        assertFalse(validator.validate(new CreateOfxParserRequest(" ", file)).isEmpty());
        assertFalse(validator.validate(new CreateOfxParserRequest("not-an-object-id", file)).isEmpty());
    }

    @Test
    void shouldRejectMissingFile() {
        assertFalse(validator.validate(new CreateOfxParserRequest(new ObjectId().toHexString(), null)).isEmpty());
    }
}
