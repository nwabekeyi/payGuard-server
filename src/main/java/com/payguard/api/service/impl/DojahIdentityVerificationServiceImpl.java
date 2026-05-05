package com.payguard.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payguard.api.config.DojahConfig;
import com.payguard.api.dto.request.IdentityVerificationRequest;
import com.payguard.api.dto.response.IdentityVerificationResponse;
import com.payguard.api.entity.User;
import com.payguard.api.entity.UserKyc;
import com.payguard.api.entity.enumeration.KycType;
import com.payguard.api.model.AccountDetail;
import com.payguard.api.repository.UserKycRepository;
import com.payguard.api.repository.UserRepository;
import com.payguard.api.service.BankService;
import com.payguard.api.service.IdentityVerificationService;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DojahIdentityVerificationServiceImpl implements IdentityVerificationService {

    private final DojahConfig config; // kept for future real provider integration
    private final UserRepository userRepository;
    private final UserKycRepository userKycRepository;
    private final BankService bankService;

    @Override
    @Transactional
    public IdentityVerificationResponse verifyIdentity(IdentityVerificationRequest request, User currentUser) {
        KycType kycType;
        try {
            kycType = KycType.valueOf(request.getKycType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IdentityVerificationResponse.builder().verified(false).message("Unsupported KYC Type").matchScore("0%").build();
        }

        // Mock verification until provider keys are available
        boolean verified = isMockIdentityValid(kycType, request.getKycId());
        if (!verified) {
            return IdentityVerificationResponse.builder().verified(false).message("Mock verification failed").matchScore("0%").build();
        }

        String bankName = null;
        try {
            AccountDetail detail = bankService.resolveAccount(request.getBankAccountNumber(), request.getBankCode());
            bankName = detail.getAccountName();
        } catch (Exception e) {
            log.warn("Account resolution failed during mock KYC", e);
        }

        currentUser.setIdentityVerified(true);
        currentUser.setKycType(kycType);
        currentUser.setKycId(request.getKycId());
        currentUser.setBankAccountNumber(request.getBankAccountNumber());
        currentUser.setBankCode(request.getBankCode());
        currentUser.setBankName(bankName);

        UserKyc userKyc = userKycRepository.findByUser(currentUser).orElse(UserKyc.builder().user(currentUser).build());
        userKyc.setKycType(kycType);
        userKyc.setKycId(request.getKycId());
        userKyc.setBankAccountNumber(request.getBankAccountNumber());
        userKyc.setBankCode(request.getBankCode());
        userKyc.setBankName(bankName);
        userKyc.setVerified(true);
        userKyc.setProvider("DOJAH_MOCK");
        userKyc.setVerificationReference("MOCK-" + UUID.randomUUID());

        userRepository.save(currentUser);
        userKycRepository.save(userKyc);

        return IdentityVerificationResponse.builder()
                .verified(true)
                .message("Mock " + kycType + " verification successful")
                .matchScore("100%")
                .build();
    }

    private boolean isMockIdentityValid(KycType type, String kycId) {
        if (kycId == null || kycId.length() != 11 || !kycId.chars().allMatch(Character::isDigit)) return false;
        // deterministic mock rule: BVN ends with even digit, NIN ends with odd digit
        int last = Character.getNumericValue(kycId.charAt(kycId.length() - 1));
        return type == KycType.BVN ? last % 2 == 0 : last % 2 == 1;
    }
}
