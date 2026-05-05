package com.payguard.service;

import com.payguard.dto.request.ResolveDisputeRequest;
import com.payguard.dto.response.AdminDashboardResponse;
import com.payguard.dto.response.DisputeResponse;
import com.payguard.entity.User;

import java.util.UUID;

public interface AdminEscrowService {
    AdminDashboardResponse getDashboard();
    DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, User adminUser);
}
