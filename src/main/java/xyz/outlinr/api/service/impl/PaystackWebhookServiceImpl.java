package com.payguard.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payguard.entity.Escrow;
import com.payguard.entity.enumeration.EscrowStatus;
import com.payguard.entity.enumeration.LedgerStatus;
import com.payguard.repository.EscrowRepository;
import com.payguard.repository.FinancialLedgerRepository;
import com.payguard.service.PaystackWebhookService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackWebhookServiceImpl implements PaystackWebhookService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EscrowRepository escrowRepository;
    private final FinancialLedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paystack.secret-key}")
    private String paystackSecret;

    @Override
    @Transactional
    public void processWebhook(String signature, String payload) {
        if (!isValidSignature(signature, payload)) {
            throw new SecurityException("Invalid paystack webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook payload", e);
        }

        String event = root.path("event").asText();
        String eventId = root.path("data").path("id").asText();
        if (eventId.isBlank()) eventId = root.path("data").path("reference").asText();

        String idemKey = "paystack:webhook:" + event + ":" + eventId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idemKey, "processed", Duration.ofDays(7));
        if (Boolean.FALSE.equals(first)) {
            log.info("Duplicate webhook ignored: {}", idemKey);
            return;
        }

        if ("charge.success".equals(event)) {
            String escrowIdText = root.path("data").path("metadata").path("escrowId").asText();
            if (!escrowIdText.isBlank() && !"new".equalsIgnoreCase(escrowIdText)) {
                Escrow escrow = escrowRepository.findByIdForUpdate(UUID.fromString(escrowIdText)).orElse(null);
                if (escrow != null && escrow.getStatus() == EscrowStatus.AWAITING_FUNDING) {
                    escrow.setStatus(EscrowStatus.FUNDED);
                    escrowRepository.save(escrow);
                }
            }
        } else if ("refund.processed".equals(event)) {
            String reference = root.path("data").path("transaction_reference").asText();
            ledgerRepository.findByPaymentReference(reference).ifPresent(l -> {
                l.setStatus(LedgerStatus.REFUNDED);
                ledgerRepository.save(l);
            });
        } else if ("transfer.success".equals(event)) {
            String reference = root.path("data").path("reference").asText();
            ledgerRepository.findByPaymentReference(reference).ifPresent(l -> {
                l.setStatus(LedgerStatus.RELEASED);
                ledgerRepository.save(l);
            });
        }
    }

    private boolean isValidSignature(String signature, String payload) {
        try {
            Mac sha512 = Mac.getInstance("HmacSHA512");
            sha512.init(new SecretKeySpec(paystackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String computed = HexFormat.of().formatHex(sha512.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
