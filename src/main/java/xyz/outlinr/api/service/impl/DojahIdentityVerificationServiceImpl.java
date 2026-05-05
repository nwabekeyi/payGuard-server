package xyz.outlinr.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import xyz.outlinr.api.config.DojahConfig;
import xyz.outlinr.api.dto.request.IdentityVerificationRequest;
import xyz.outlinr.api.dto.response.IdentityVerificationResponse;
import xyz.outlinr.api.entity.User;
import xyz.outlinr.api.entity.enumeration.KycType;
import xyz.outlinr.api.model.AccountDetail;
import xyz.outlinr.api.repository.UserRepository;
import xyz.outlinr.api.service.BankService;
import xyz.outlinr.api.service.IdentityVerificationService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DojahIdentityVerificationServiceImpl implements IdentityVerificationService {

    private final DojahConfig config;
    private final UserRepository userRepository;
    private final BankService bankService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public IdentityVerificationResponse verifyIdentity(IdentityVerificationRequest request, User currentUser) {
        if (currentUser.isIdentityVerified()) {
            return IdentityVerificationResponse.builder()
                    .verified(true)
                    .message("Identity is already verified.")
                    .build();
        }

        KycType kycType;
        try {
            kycType = KycType.valueOf(request.getKycType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IdentityVerificationResponse.builder()
                    .verified(false)
                    .message("Unsupported KYC Type: " + request.getKycType())
                    .build();
        }

        String kycId = request.getKycId();
        boolean isVerified = false;
        String message = "Verification failed";
        String matchScore = "0%";

        try {
            if (kycType == KycType.BVN) {
                // Verify BVN using Dojah
                Map<String, Object> bvnData = verifyBvnWithDojah(kycId);
                if (bvnData != null) {
                    // Verify bank account matches if provided
                    boolean accountMatch = true;
                    if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isEmpty()
                            && request.getBankCode() != null && !request.getBankCode().isEmpty()) {
                        // Optionally verify account matches using Dojah account validation or Paystack
                        // For now, we'll use Paystack to resolve and compare names if needed
                        accountMatch = verifyAccountMatchesIdentity(request.getBankAccountNumber(), request.getBankCode(), bvnData);
                    }

                    if (!accountMatch) {
                        return IdentityVerificationResponse.builder()
                                .verified(false)
                                .message("Bank account does not match provided identity")
                                .matchScore("0%")
                                .build();
                    }

                    // Update user
                    currentUser.setIdentityVerified(true);
                    currentUser.setKycType(kycType);
                    currentUser.setKycId(kycId);
                    currentUser.setBankAccountNumber(request.getBankAccountNumber());
                    currentUser.setBankCode(request.getBankCode());

                    // Resolve account name using Paystack (still allowed for account resolution)
                    if (request.getBankAccountNumber() != null && request.getBankCode() != null) {
                        try {
                            AccountDetail detail = bankService.resolveAccount(request.getBankAccountNumber(), request.getBankCode());
                            currentUser.setBankName(detail.getAccountName());
                            log.info("Resolved account name: {}", detail.getAccountName());
                        } catch (Exception e) {
                            log.warn("Could not resolve account name", e);
                        }
                    }

                    userRepository.save(currentUser);

                    isVerified = true;
                    message = "BVN verification successful";
                    matchScore = "100%";
                } else {
                    message = "BVN verification failed: Invalid response from verification service";
                }
            } else if (kycType == KycType.NIN) {
                // Verify NIN using Dojah
                Map<String, Object> ninData = verifyNinWithDojah(kycId);
                if (ninData != null) {
                    boolean accountMatch = true;
                    if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isEmpty()
                            && request.getBankCode() != null && !request.getBankCode().isEmpty()) {
                        accountMatch = verifyAccountMatchesIdentity(request.getBankAccountNumber(), request.getBankCode(), ninData);
                    }

                    if (!accountMatch) {
                        return IdentityVerificationResponse.builder()
                                .verified(false)
                                .message("Bank account does not match provided identity")
                                .matchScore("0%")
                                .build();
                    }

                    currentUser.setIdentityVerified(true);
                    currentUser.setKycType(kycType);
                    currentUser.setKycId(kycId);
                    currentUser.setBankAccountNumber(request.getBankAccountNumber());
                    currentUser.setBankCode(request.getBankCode());

                    if (request.getBankAccountNumber() != null && request.getBankCode() != null) {
                        try {
                            AccountDetail detail = bankService.resolveAccount(request.getBankAccountNumber(), request.getBankCode());
                            currentUser.setBankName(detail.getAccountName());
                            log.info("Resolved account name: {}", detail.getAccountName());
                        } catch (Exception e) {
                            log.warn("Could not resolve account name", e);
                        }
                    }

                    userRepository.save(currentUser);

                    isVerified = true;
                    message = "NIN verification successful";
                    matchScore = "100%";
                } else {
                    message = "NIN verification failed: Invalid response from verification service";
                }
            } else {
                message = "Unsupported KYC type";
            }
        } catch (Exception e) {
            log.error("Error during identity verification with Dojah", e);
            message = "Verification error: " + e.getMessage();
        }

        return IdentityVerificationResponse.builder()
                .verified(isVerified)
                .message(message)
                .matchScore(matchScore)
                .build();
    }

    private Map<String, Object> verifyBvnWithDojah(String bvn) {
        String url = config.getBaseUrl() + "/api/v1/kyc/bvn";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client-id", config.getClientId());
        headers.set("client-secret", config.getClientSecret());
        
        Map<String, String> payload = Map.of(
            "bvn", bvn,
            "businessRefId", config.getBusinessRefId()
        );
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            log.info("Dojah BVN verification response: {}", body);

            if (body != null && "success".equalsIgnoreCase((String) body.get("status"))) {
                return (Map<String, Object>) body.get("data");
            }
            return null;
        } catch (Exception e) {
            log.error("Error calling Dojah BVN API", e);
            throw new RuntimeException("Failed to verify BVN", e);
        }
    }

    private Map<String, Object> verifyNinWithDojah(String nin) {
        String url = config.getBaseUrl() + "/api/v1/kyc/nin";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client-id", config.getClientId());
        headers.set("client-secret", config.getClientSecret());
        
        Map<String, String> payload = Map.of(
            "nin", nin,
            "businessRefId", config.getBusinessRefId()
        );
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            log.info("Dojah NIN verification response: {}", body);

            if (body != null && "success".equalsIgnoreCase((String) body.get("status"))) {
                return (Map<String, Object>) body.get("data");
            }
            return null;
        } catch (Exception e) {
            log.error("Error calling Dojah NIN API", e);
            throw new RuntimeException("Failed to verify NIN", e);
        }
    }

    private boolean verifyAccountMatchesIdentity(String accountNumber, String bankCode, Map<String, Object> identityData) {
        try {
            // Extract name from Dojah response - field names may vary based on Dojah's actual response
            String firstName = (String) identityData.getOrDefault("firstName", 
                            identityData.getOrDefault("first_name", ""));
            String lastName = (String) identityData.getOrDefault("lastName", 
                           identityData.getOrDefault("last_name", ""));
            String fullNameFromDojah = (firstName + " " + lastName).trim().toLowerCase();
            
            // Also check for a direct fullName field
            String directFullName = (String) identityData.getOrDefault("fullName", 
                                  identityData.getOrDefault("full_name", ""));
            if (!directFullName.isEmpty()) {
                fullNameFromDojah = directFullName.toLowerCase();
            }
            
            if (fullNameFromDojah.isEmpty()) {
                log.warn("Could not extract name from Dojah response: {}", identityData);
                return false;
            }

            // Resolve account name from Paystack
            AccountDetail accountDetail = bankService.resolveAccount(accountNumber, bankCode);
            String accountName = accountDetail.getAccountName().toLowerCase();
            
            log.info("Comparing Dojah name '{}' with Paystack account name '{}'", 
                    fullNameFromDojah, accountName);
            
            // Simple matching: check if account name contains the Dojah name or vice versa
            // This handles cases like "John Doe" vs "DOE JOHN" or "John A. Doe"
            boolean match = accountName.contains(fullNameFromDojah) || 
                           fullNameFromDojah.contains(accountName) ||
                           areNamesSimilar(fullNameFromDojah, accountName);
            
            if (!match) {
                log.warn("Name mismatch: Dojah='{}', Paystack='{}'", fullNameFromDojah, accountName);
            }
            
            return match;
        } catch (Exception e) {
            log.error("Error verifying account match", e);
            return false;
        }
    }
    
    // Simple similarity check: compare tokens (name parts)
    private boolean areNamesSimilar(String name1, String name2) {
        String[] tokens1 = name1.toLowerCase().split("\\s+");
        String[] tokens2 = name2.toLowerCase().split("\\s+");
        
        int matchCount = 0;
        for (String token1 : tokens1) {
            for (String token2 : tokens2) {
                if (token1.equals(token2) || token1.contains(token2) || token2.contains(token1)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        // Consider it a match if at least 2 tokens match or one token matches and it's a unique name
        return matchCount >= 2 || (matchCount == 1 && tokens1.length <= 2 && tokens2.length <= 2);
    }
}