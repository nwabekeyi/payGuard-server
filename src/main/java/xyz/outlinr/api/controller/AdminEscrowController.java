package com.payguard.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.payguard.api.dto.request.ResolveDisputeRequest;
import com.payguard.api.dto.response.AdminDashboardResponse;
import com.payguard.api.dto.response.DisputeResponse;
import com.payguard.api.entity.User;
import com.payguard.api.service.AdminEscrowService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/escrow")
@RequiredArgsConstructor
public class AdminEscrowController {
    private final AdminEscrowService adminEscrowService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminEscrowService.getDashboard());
    }

    @PostMapping("/disputes/{disputeId}/resolve")
    public ResponseEntity<DisputeResponse> resolveDispute(@PathVariable UUID disputeId,
                                                          @RequestBody ResolveDisputeRequest request,
                                                          @AuthenticationPrincipal User adminUser) {
        return ResponseEntity.ok(adminEscrowService.resolveDispute(disputeId, request, adminUser));
    }
}
