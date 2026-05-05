package com.payguard.api.service;

import com.payguard.api.dto.request.IdentityVerificationRequest;
import com.payguard.api.dto.response.IdentityVerificationResponse;
import com.payguard.api.entity.User;

public interface IdentityVerificationService {
    IdentityVerificationResponse verifyIdentity(IdentityVerificationRequest request, User currentUser);
}
