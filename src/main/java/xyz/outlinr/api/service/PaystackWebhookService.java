package com.payguard.service;

public interface PaystackWebhookService {
    void processWebhook(String signature, String payload);
}
