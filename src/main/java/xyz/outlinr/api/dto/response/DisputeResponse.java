package com.payguard.api.dto.response;

import com.payguard.api.entity.enumeration.DisputeResolution;
import com.payguard.api.entity.enumeration.DisputeStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID escrowId,
        String raisedBy,
        String reason,
        DisputeStatus status,
        DisputeResolution resolution,
        String adminNotes,
        Instant deadline,
        Instant createdAt,
        Instant updatedAt,
        List<DisputeEvidenceResponse> evidence
) {}
