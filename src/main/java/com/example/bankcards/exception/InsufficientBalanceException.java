package com.example.bankcards.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientBalanceException(BigDecimal required, BigDecimal available) {
        super(String.format(
                "Insufficient balance. Required: %s, Available: %s, Shortage: %s",
                required,
                available,
                required.subtract(available)
        ));
        this.required = required;
        this.available = available;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getShortage() {
        return required.subtract(available);
    }
}
