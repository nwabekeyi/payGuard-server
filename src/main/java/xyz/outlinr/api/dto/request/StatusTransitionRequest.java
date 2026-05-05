package com.payguard.api.dto.request;

import com.payguard.api.entity.enumeration.EscrowStatus;

public record StatusTransitionRequest(
        EscrowStatus toStatus,
        String reason) {
}
