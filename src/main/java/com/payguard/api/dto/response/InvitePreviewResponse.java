package com.payguard.api.dto.response;

import com.payguard.api.entity.enumeration.DeliveryType;
import com.payguard.api.entity.enumeration.ParticipantRole;

import java.math.BigDecimal;

public record InvitePreviewResponse(
        String title,
        String description,
        BigDecimal amount,
        String currency,
        DeliveryType deliveryType,
        String createdByName,
        ParticipantRole invitedRole) {
}

