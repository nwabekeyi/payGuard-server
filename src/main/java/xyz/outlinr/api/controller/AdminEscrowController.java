package com.payguard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.payguard.dto.request.ResolveDisputeRequest;
import com.payguard.dto.response.AdminDashboardResponse;
import com.payguard.dto.response.DisputeResponse;
import com.payguard.entity.User;
import com.payguard.service.AdminEscrowService;

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
