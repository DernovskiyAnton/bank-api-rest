package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequestDto(
        @NotNull(message = "Source card ID is required")
        Long fromCardId,

        @NotNull(message = "Destination card ID is required")
        Long toCardId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        String description
) {
}