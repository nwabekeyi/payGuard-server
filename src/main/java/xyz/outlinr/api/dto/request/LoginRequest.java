package com.payguard.api.dto.request;

public record LoginRequest(
        String email,
        String password) {
}
