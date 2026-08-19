package com.samuelfilho_dev.finance_module.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionsTest {

    @Test
    void shouldExposeMessage() {
        assertEquals("not found", new NotFoundException("not found").getMessage());
        assertEquals("business", new BusinessException("business").getMessage());
        assertEquals("forbidden", new ForbiddenException("forbidden").getMessage());
        assertEquals("unauthorized", new UnauthorizedException("unauthorized").getMessage());
        assertEquals("ofx", new OfxException("ofx").getMessage());
    }

    @Test
    void ofxException_shouldKeepCause() {
        var cause = new IllegalArgumentException("bad");
        var exception = new OfxException("ofx", cause);
        assertEquals("ofx", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
