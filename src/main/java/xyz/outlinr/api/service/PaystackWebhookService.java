package com.payguard.api.service;

public interface PaystackWebhookService {
    void processWebhook(String signature, String payload);
}
