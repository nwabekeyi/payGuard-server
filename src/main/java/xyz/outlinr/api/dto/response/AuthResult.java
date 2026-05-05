package com.payguard.dto.response;

public record AuthResult(
        String accessToken,
        String refreshToken,
        AuthResponse response) {
}
