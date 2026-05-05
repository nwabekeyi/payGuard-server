package com.payguard.api.dto.response;

import com.payguard.api.entity.enumeration.ParticipantRole;

import java.time.Instant;
import java.util.UUID;

public record EscrowParticipantResponse(
        UUID id,
        String email,
        String name,
        ParticipantRole role,
        Boolean inviteAccepted,
        Instant inviteAcceptedAt) {
}
