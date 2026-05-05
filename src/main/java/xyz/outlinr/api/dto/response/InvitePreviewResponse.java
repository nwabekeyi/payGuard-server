package com.payguard.dto.response;

import com.payguard.entity.enumeration.DeliveryType;
import com.payguard.entity.enumeration.ParticipantRole;

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

