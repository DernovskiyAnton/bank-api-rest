package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardDto(
        Long id,
        String cardNumber,
        String cardholderName,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance
) {
}