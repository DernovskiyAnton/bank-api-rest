package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockCardRequestDto(
        @NotBlank(message = "Reason is required")
        @Size(max = 500, message = "Reason must be less than 500 characters")
        String reason
) {
}
