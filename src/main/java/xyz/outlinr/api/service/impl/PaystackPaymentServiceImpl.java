package com.payguard.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.payguard.api.config.PaystackConfig;
import com.payguard.api.dto.request.CreateEscrowRequest;
import com.payguard.api.dto.request.VerifyAndCreateEscrowRequest;
import com.payguard.api.entity.Escrow;
import com.payguard.api.entity.EscrowStatusHistory;
import com.payguard.api.entity.FinancialLedger;
import com.payguard.api.entity.User;
import com.payguard.api.entity.enumeration.EscrowStatus;
import com.payguard.api.entity.enumeration.LedgerStatus;
import com.payguard.api.dto.response.InitPaymentResponse;
import com.payguard.api.dto.response.VerifyPaymentResponse;
import com.payguard.api.repository.EscrowRepository;
import com.payguard.api.repository.FinancialLedgerRepository;
import com.payguard.api.service.EmailService;
import com.payguard.api.service.PaymentService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaystackPaymentServiceImpl implements PaymentService {

    private final PaystackConfig config;
    private final EscrowRepository escrowRepository;
    private final FinancialLedgerRepository ledgerRepository;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;
    @Value("${app.fees.platform.percentage:1.50}")
    private BigDecimal platformFeePercentage;
    @Value("${app.fees.platform.fixed-ngn:0}")
    private BigDecimal platformFixedFee;
    @Value("${app.fees.paystack.percentage:1.50}")
    private BigDecimal paystackFeePercentage;
    @Value("${app.fees.paystack.fixed-ngn:100}")
    private BigDecimal paystackFixedFee;
    @Value("${app.fees.paystack.cap-ngn:2000}")
    private BigDecimal paystackFeeCap;

    @Override
    @Transactional
    public InitPaymentResponse initiatePayment(UUID escrowId, User currentUser) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new NoSuchElementException("Escrow not found: " + escrowId));

        if (escrow.getStatus() != EscrowStatus.AWAITING_FUNDING && escrow.getStatus() != EscrowStatus.DRAFT) {
            throw new IllegalStateException("Escrow cannot be funded in its current state: " + escrow.getStatus());
        }

        long amountInKobo = escrow.getAmount().multiply(new BigDecimal(100)).longValue();
        return initiatePaymentCommon(escrowId.toString(), amountInKobo, currentUser);
    }

    @Override
    @Transactional
    public InitPaymentResponse initiateNewEscrowPayment(CreateEscrowRequest request, User currentUser) {
        long amountInKobo = request.amount().multiply(new BigDecimal(100)).longValue();
        // Use a temporary identifier; we will handle atomic creation via verify-and-create
        return initiatePaymentCommon("new", amountInKobo, currentUser);
    }

    private InitPaymentResponse initiatePaymentCommon(String siteId, long amountInKobo, User currentUser) {
        String callbackUrl = frontendUrl + "/verify-payment/" + siteId;

        // Build payload
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("amount", amountInKobo);
        payload.put("email", currentUser.getEmail());
        payload.put("currency", "NGN");
        payload.put("callback_url", callbackUrl);
        // Unique reference
        String reference = "REF-" + com.payguard.api.utils.UUIDv7Generator.generate();
        payload.put("reference", reference);
        // Metadata to identify escrow when verifying webhook/callback
        Map<String, String> metadata = Map.of("escrowId", siteId);
        payload.put("metadata", metadata);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getSecretKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        String url = config.getBaseUrl() + "/transaction/initialize";

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("status"))) {
                throw new IllegalStateException("Paystack initialization failed: " + (body != null ? body.get("message") : "No response"));
            }

            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String accessCode = (String) data.get("access_code");
            String referenceResp = (String) data.get("reference");

            return InitPaymentResponse.builder()
                    .accessCode(accessCode)
                    .reference(referenceResp)
                    .email(currentUser.getEmail())
                    .amount(String.valueOf(amountInKobo))
                    .currency("NGN")
                    .callbackUrl(callbackUrl)
                    .build();

        } catch (Exception e) {
            log.error("Paystack payment initiation failed", e);
            throw new RuntimeException("Could not initiate payment: " + e.getMessage(), e);
        }
    }

    // This verifies the transaction with Paystack and funds the escrow if success
    @Override
    @Transactional
    public boolean verifyAndFundEscrow(UUID escrowId, String transactionReference, long amountInKobo) {
        String idempotencyKey = "payment:verify:" + transactionReference;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "PROCESSING", java.time.Duration.ofMinutes(30));
        if (Boolean.FALSE.equals(first)) {
            log.info("Skipping duplicate verify request for reference {}", transactionReference);
            return true;
        }

        Escrow escrow = escrowRepository.findByIdForUpdate(escrowId)
                .orElseThrow(() -> new NoSuchElementException("Escrow not found: " + escrowId));

        // Already funded?
        if (escrow.getStatus() == EscrowStatus.PENDING_ACCEPTANCE ||
                escrow.getStatus() == EscrowStatus.ACTIVE ||
                escrow.getStatus() == EscrowStatus.FUNDED) {
            return true;
        }

        boolean success = verifyTransactionWithPaystack(transactionReference, amountInKobo);
        if (success) {
            log.info("Payment Verification SUCCESS for escrow {}", escrowId);

            EscrowStatus fromStatus = escrow.getStatus();

            boolean sellerAccepted = escrow.getParticipants().stream()
                    .filter(p -> p.getRole() == com.payguard.api.entity.enumeration.ParticipantRole.SELLER)
                    .anyMatch(p -> p.getInviteAccepted() != null && p.getInviteAccepted());

            EscrowStatus nextStatus = sellerAccepted ? EscrowStatus.FUNDED : EscrowStatus.PENDING_ACCEPTANCE;
            escrow.setStatus(nextStatus);

            EscrowStatusHistory history = EscrowStatusHistory.builder()
                    .escrow(escrow)
                    .fromStatus(fromStatus)
                    .toStatus(nextStatus)
                    .changedBy(escrow.getCreatedBy())
                    .reason("Payment verified via Paystack: " + transactionReference)
                    .build();

            escrow.getStatusHistory().add(history);
            escrowRepository.save(escrow);

            if (ledgerRepository.findByEscrow(escrow).isEmpty()) {
                BigDecimal grossAmount = escrow.getAmount();
                BigDecimal paystackFee = computePaystackFee(grossAmount);
                BigDecimal platformFee = computePlatformFee(grossAmount);
                BigDecimal netPayout = grossAmount.subtract(paystackFee).subtract(platformFee).max(BigDecimal.ZERO);
                FinancialLedger ledger = FinancialLedger.builder()
                        .escrow(escrow)
                        .amount(grossAmount)
                        .paystackFee(paystackFee)
                        .platformFee(platformFee)
                        .netPayoutAmount(netPayout)
                        .currency(escrow.getCurrency())
                        .status(LedgerStatus.HELD)
                        .paymentReference(transactionReference)
                        .build();
                ledgerRepository.save(ledger);
            }

            try {
                emailService.sendEscrowInviteEmails(escrow, escrow.getCreatedBy());
            } catch (Exception e) {
                log.error("Failed to send escrow invite emails", e);
            }

            return true;
        }

        log.warn("Payment Verification FAILED for escrow {}", escrowId);
        return false;
    }

    private BigDecimal computePaystackFee(BigDecimal amount) {
        BigDecimal percentageFee = amount.multiply(paystackFeePercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal fee = percentageFee.add(paystackFixedFee);
        if (paystackFeeCap != null && paystackFeeCap.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(paystackFeeCap) > 0) {
            fee = paystackFeeCap;
        }
        return fee.max(BigDecimal.ZERO);
    }

    private BigDecimal computePlatformFee(BigDecimal amount) {
        BigDecimal pct = amount.multiply(platformFeePercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return pct.add(platformFixedFee).max(BigDecimal.ZERO);
    }

    private boolean verifyTransactionWithPaystack(String reference, long expectedAmountKobo) {
        String url = config.getBaseUrl() + "/transaction/verify/" + reference;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getSecretKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.TRUE.equals(body.get("status")) && body.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                String paystackStatus = (String) data.get("status");
                boolean paid = "success".equalsIgnoreCase(paystackStatus) || "success".equalsIgnoreCase((String) data.get("payment_status"));
                if (paid) {
                    Number amountNum = (Number) data.get("amount"); // amount in kobo from Paystack
                    long amountPaid = amountNum != null ? amountNum.longValue() : -1;
                    if (amountPaid == expectedAmountKobo) {
                        return true;
                    } else {
                        log.warn("Amount mismatch: expected {} kobo, got {} kobo", expectedAmountKobo, amountPaid);
                    }
                } else {
                    log.info("Paystack payment status not successful: {}", paystackStatus);
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error during Paystack transaction verification", e);
            return false;
        }
    }

    @Override
    public VerifyPaymentResponse verifyEscrowPayment(UUID escrowId, String transactionReference, long amountInKobo) {
        boolean isSuccessful = verifyAndFundEscrow(escrowId, transactionReference, amountInKobo);
        if (isSuccessful) {
            return new VerifyPaymentResponse("success", "Payment verified and escrow funded");
        }
        return new VerifyPaymentResponse("failed", "Payment verification failed");
    }

    @Override
    public boolean verifyTransaction(String transactionReference) {
        // Not used directly; return true placeholder
        return true;
    }

    @Override
    public VerifyPaymentResponse verifyAndCreateEscrow(VerifyAndCreateEscrowRequest request, User currentUser) {
        // This should verify and then create escrow. But flow uses verifyAndCreate endpoint;
        // We'll just verify and if successful, the controller will handle creating escrow separately.
        boolean success = verifyTransactionWithPaystack(request.txnRef(), request.amount());
        if (success) {
            return new VerifyPaymentResponse("success", "Payment verified");
        }
        return new VerifyPaymentResponse("failed", "Payment verification failed");
    }
}
