package com.samuelfilho_dev.finance_module.exceptions;

public class OfxException extends RuntimeException {
    public OfxException(String message) {
        super(message);
    }

    public OfxException(String message, Throwable cause) {
        super(message, cause);
    }
}
