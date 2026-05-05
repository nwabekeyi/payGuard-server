package com.payguard.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.payguard.api.service.PaystackWebhookService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook/paystack")
@RequiredArgsConstructor
public class PaystackWebhookController {

    private final PaystackWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@RequestHeader("x-paystack-signature") String signature,
                                                       @RequestBody String payload) {
        webhookService.processWebhook(signature, payload);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
