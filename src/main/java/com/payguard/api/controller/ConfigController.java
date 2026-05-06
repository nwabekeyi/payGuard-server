package com.payguard.api.controller;

import com.payguard.api.service.PaystackCurrencyService;
import com.payguard.api.utils.EscrowDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @GetMapping("/escrow")
    public ResponseEntity<Map<String, Object>> getEscrowConfig() {
        Map<String, Object> config = Map.ofEntries(
            Map.entry("currency", EscrowDefaults.DEFAULT_CURRENCY),
            Map.entry("currencies", PaystackCurrencyService.getCurrencies()),
            Map.entry("inspectionPeriodDays", EscrowDefaults.INSPECTION_PERIOD_DAYS),
            Map.entry("autoRelease", EscrowDefaults.AUTO_RELEASE),
            Map.entry("disputeWindowHours", EscrowDefaults.DISPUTE_WINDOW_HOURS),
            Map.entry("requireProofOfDelivery", EscrowDefaults.REQUIRE_PROOF_OF_DELIVERY),
            Map.entry("milestoneEnabled", EscrowDefaults.MILESTONE_ENABLED),
            Map.entry("amountMin", EscrowDefaults.AMOUNT_MIN),
            Map.entry("amountMax", EscrowDefaults.AMOUNT_MAX),
            Map.entry("feePercent", EscrowDefaults.FEE_PERCENT)
        );
        return ResponseEntity.ok(config);
    }
}


