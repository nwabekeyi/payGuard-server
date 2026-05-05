package com.payguard.service;

import com.payguard.dto.request.IdentityVerificationRequest;
import com.payguard.dto.response.IdentityVerificationResponse;
import com.payguard.entity.User;

public interface IdentityVerificationService {
    IdentityVerificationResponse verifyIdentity(IdentityVerificationRequest request, User currentUser);
}
