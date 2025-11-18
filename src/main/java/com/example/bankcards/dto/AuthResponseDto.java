package com.example.bankcards.dto;

public record AuthResponseDto(
        String token,
        String tokenType,
        UserDto user
) {

    public static AuthResponseDto of(String token, UserDto user) {
        return new AuthResponseDto(token, "Bearer", user);
    }
}