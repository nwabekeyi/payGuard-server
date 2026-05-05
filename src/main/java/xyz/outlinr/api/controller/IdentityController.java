package com.payguard.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.payguard.dto.request.IdentityVerificationRequest;
import com.payguard.dto.response.IdentityVerificationResponse;
import com.payguard.entity.User;
import com.payguard.service.IdentityVerificationService;

@Slf4j
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityVerificationService identityVerificationService;

    @PostMapping("/verify")
    public ResponseEntity<IdentityVerificationResponse> verifyIdentity(
            @Valid @RequestBody IdentityVerificationRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Received identity verification request for user {}", currentUser.getEmail());
        IdentityVerificationResponse response = identityVerificationService.verifyIdentity(request, currentUser);
        return ResponseEntity.ok(response);
    }
}
