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

        // Validate KYC ID format
        if (request.getKycId() == null || request.getKycId().length() != 11 || !request.getKycId().chars().allMatch(Character::isDigit)) {
            return IdentityVerificationResponse.builder().verified(false).message("Invalid KYC ID").matchScore("0%").build();
        }

        // Mock verification until provider keys are available
        boolean verified = isMockIdentityValid(kycType, request.getKycId());
        if (!verified) {
            return IdentityVerificationResponse.builder().verified(false).message("Mock verification failed").matchScore("0%").build();
        }

        // Bank handling: only process if user doesn't already have a bank account OR if new details are provided
        String bankAccountNumber = currentUser.getBankAccountNumber();
        String bankCode = currentUser.getBankCode();
        String bankName = currentUser.getBankName();

        boolean hasExistingBank = bankAccountNumber != null && !bankAccountNumber.isBlank();

        if (!hasExistingBank) {
            // New user: bank details required
            if (request.getBankAccountNumber() == null || request.getBankAccountNumber().isBlank() ||
                request.getBankCode() == null || request.getBankCode().isBlank()) {
                return IdentityVerificationResponse.builder()
                        .verified(false)
                        .message("Bank account details are required")
                        .matchScore("0%")
                        .build();
            }
            bankAccountNumber = request.getBankAccountNumber();
            bankCode = request.getBankCode();
            try {
                AccountDetail detail = bankService.resolveAccount(bankAccountNumber, bankCode);
                bankName = detail.getAccountName();
            } catch (Exception e) {
                log.warn("Account resolution failed during mock KYC", e);
                return IdentityVerificationResponse.builder()
                        .verified(false)
                        .message("Failed to verify bank account")
                        .matchScore("0%")
                        .build();
            }
        } else {
            // Existing bank: allow optional update
            if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isBlank() &&
                request.getBankCode() != null && !request.getBankCode().isBlank()) {
                bankAccountNumber = request.getBankAccountNumber();
                bankCode = request.getBankCode();
                try {
                    AccountDetail detail = bankService.resolveAccount(bankAccountNumber, bankCode);
                    bankName = detail.getAccountName();
                } catch (Exception e) {
                    log.warn("Account resolution failed during bank update", e);
                    return IdentityVerificationResponse.builder()
                            .verified(false)
                            .message("Failed to verify new bank account")
                            .matchScore("0%")
                            .build();
                }
            }
            // else: keep existing bank details
        }

        currentUser.setIdentityVerified(true);
        currentUser.setKycType(kycType);
        currentUser.setKycId(request.getKycId());
        currentUser.setBankAccountNumber(bankAccountNumber);
        currentUser.setBankCode(bankCode);
        currentUser.setBankName(bankName);

        UserKyc userKyc = userKycRepository.findByUser(currentUser).orElse(UserKyc.builder().user(currentUser).build());
        userKyc.setKycType(kycType);
        userKyc.setKycId(request.getKycId());
        userKyc.setBankAccountNumber(bankAccountNumber);
        userKyc.setBankCode(bankCode);
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
        // For development, accept any valid 11-digit number
        return kycId != null && kycId.length() == 11 && kycId.chars().allMatch(Character::isDigit);
    }
}
