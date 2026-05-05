package com.payguard.dto.response;

import com.payguard.entity.enumeration.ConfirmationMethod;
import com.payguard.entity.enumeration.DeliveryType;
import com.payguard.entity.enumeration.EscrowStatus;
import com.payguard.entity.enumeration.ParticipationMode;
import com.payguard.entity.enumeration.ShippingResponsibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EscrowResponse(
        UUID id,
        String title,
        String description,
        BigDecimal amount,
        String currency,
        EscrowStatus status,
        ParticipationMode participationMode,
        DeliveryType deliveryType,
        Integer inspectionPeriodDays,
        Boolean autoRelease,
        Integer disputeWindowHours,
        Boolean requireProofOfDelivery,
        Boolean milestoneEnabled,
        String customDeliveryNotes,
        ShippingResponsibility shippingResponsibility,
        Integer expectedDeliveryDays,
        ConfirmationMethod confirmationMethod,
        String deliveryEvidence,
        Instant createdAt,
        Instant updatedAt,
        String createdByName,
        String createdByEmail,
        List<EscrowParticipantResponse> participants) {
}
