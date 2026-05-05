package com.payguard.api.service;

import com.payguard.api.dto.request.ResolveDisputeRequest;
import com.payguard.api.dto.response.AdminDashboardResponse;
import com.payguard.api.dto.response.DisputeResponse;
import com.payguard.api.entity.User;

import java.util.UUID;

public interface AdminEscrowService {
    AdminDashboardResponse getDashboard();
    DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, User adminUser);
}
