package com.payguard.dto.request;

import com.payguard.entity.enumeration.EscrowStatus;

public record StatusTransitionRequest(
        EscrowStatus toStatus,
        String reason) {
}
