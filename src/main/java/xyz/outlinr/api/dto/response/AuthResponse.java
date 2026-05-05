package com.payguard.dto.response;

import java.util.UUID;

public record AuthResponse(
        String message,
        UserResponse user,
        UUID escrowId) {
}
