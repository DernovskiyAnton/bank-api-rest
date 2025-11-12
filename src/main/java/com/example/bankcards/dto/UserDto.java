package com.example.bankcards.dto;

public record UserDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName
) {
}