package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        Long fromCardId,
        Long toCardId,
        BigDecimal amount,
        String status,
        String description,
        LocalDateTime createdAt
) {
}