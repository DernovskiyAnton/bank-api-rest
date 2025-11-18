package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long transactionId,
        Long fromCardId,
        Long toCardId,
        BigDecimal amount,
        String status,
        String message,
        LocalDateTime timestamp
) {
}
