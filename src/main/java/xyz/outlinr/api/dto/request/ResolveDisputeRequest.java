package com.payguard.api.dto.request;

import jakarta.validation.constraints.NotNull;
import com.payguard.api.entity.enumeration.DisputeResolution;

public record ResolveDisputeRequest(
        @NotNull(message = "Resolution is required")
        DisputeResolution resolution,
        
        String adminNotes
) {}
