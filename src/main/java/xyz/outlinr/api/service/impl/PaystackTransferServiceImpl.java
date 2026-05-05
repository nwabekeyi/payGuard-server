package com.payguard.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.payguard.api.config.PaystackConfig;
import com.payguard.api.model.AccountDetail;
import com.payguard.api.service.BankService;
import com.payguard.api.service.TransferService;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackTransferServiceImpl implements TransferService {

    private final PaystackConfig config;
    private final BankService bankService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean validateAccount(String accountNumber, String bankCode) {
        try {
            // Use bank/resolve to verify account exists and get name
            bankService.resolveAccount(accountNumber, bankCode);
            log.info("Account validation successful for {} {}", bankCode, accountNumber);
            return true;
        } catch (Exception e) {
            log.error("Account validation failed for {} {}: {}", bankCode, accountNumber, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean initiateTransfer(BigDecimal amount, String description, String toAccount, String bankCode, String txnRef, String accountName) {
        log.info("Initiating Paystack transfer of {} to account {} at bank {} (Name: {})",
                amount, toAccount, bankCode, accountName);

        try {
            // 1. Create or ensure recipient exists
            String recipientCode = createOrGetRecipient(accountName, toAccount, bankCode);

            // 2. Initiate transfer
            String url = config.getBaseUrl() + "/transfer";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(config.getSecretKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("source", "balance");
            payload.put("amount", amount.multiply(new BigDecimal(100)).intValue()); // kobo
            payload.put("recipient", recipientCode);
            payload.put("reason", description);
            payload.put("reference", txnRef);
            // optional: currency "NGN"

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("status")) && body.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                String transferStatus = (String) data.get("status");
                // "success", "processing", "pending", etc.
                if ("success".equalsIgnoreCase(transferStatus) || "processing".equalsIgnoreCase(transferStatus)) {
                    log.info("Paystack transfer initiated successfully. Ref: {}", txnRef);
                    return true;
                } else {
                    log.warn("Paystack transfer failed with status: {}, message: {}", transferStatus, data.get("message"));
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to initiate Paystack transfer", e);
            return false;
        }
    }

    private String createOrGetRecipient(String name, String accountNumber, String bankCode) {
        // For simplicity, create a new recipient every time. Could store recipient_code for future use.
        String url = config.getBaseUrl() + "/transferrecipient";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getSecretKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "nuban");
        payload.put("name", name);
        payload.put("account_number", accountNumber);
        payload.put("bank_code", bankCode);
        payload.put("currency", "NGN");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("status")) && body.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return (String) data.get("recipient_code");
            } else {
                throw new RuntimeException("Failed to create transfer recipient: " + (body != null ? body.get("message") : "No response"));
            }
        } catch (Exception e) {
            log.error("Error creating transfer recipient", e);
            throw new RuntimeException("Could not create transfer recipient", e);
        }
    }
}
