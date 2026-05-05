package com.payguard.dto.response;

public record AdminDashboardResponse(
        long totalEscrows,
        long activeEscrows,
        long disputedEscrows,
        long openDisputes,
        long resolvedDisputes,
        long pendingPayoutLedgers
) {
}
