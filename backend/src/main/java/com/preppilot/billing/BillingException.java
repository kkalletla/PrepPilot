package com.preppilot.billing;

public class BillingException extends RuntimeException {
    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
