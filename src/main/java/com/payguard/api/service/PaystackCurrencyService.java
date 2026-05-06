package com.payguard.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Order(1)
public class PaystackCurrencyService implements CommandLineRunner {
    private static final List<String> CURRENCIES = new ArrayList<>();
    private static volatile boolean loaded = false;
    @Value("${paystack.secret-key:#{null}}")
    private String paystackSecretKey;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) {
        loadCurrencies();
    }

    public void loadCurrencies() {
        if (loaded && !CURRENCIES.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (paystackSecretKey != null && !paystackSecretKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + paystackSecretKey);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            Map<String, Object> response = restTemplate.exchange(
                "https://api.paystack.co/integration/currency",
                org.springframework.http.HttpMethod.GET, entity, Map.class
            ).getBody();
            if (response != null && response.get("data") instanceof List) {
                List<?> data = (List<?>) response.get("data");
                List<String> currencyList = new ArrayList<>();
                for (Object item : data) {
                    if (item instanceof Map) {
                        Object code = ((Map<?, ?>) item).get("currencyCode");
                        if (code != null) currencyList.add(code.toString().trim().toUpperCase());
                    }
                }
                if (!currencyList.isEmpty()) {
                    CURRENCIES.clear();
                    CURRENCIES.addAll(currencyList);
                    loaded = true;
                    System.out.println("[PaystackCurrencyService] Loaded " + CURRENCIES.size() + " currencies");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("[PaystackCurrencyService] Fetch failed: " + e.getMessage() + ". Using defaults.");
        }
        if (CURRENCIES.isEmpty()) {
            CURRENCIES.addAll(List.of("NGN", "USD", "GBP", "EUR"));
            loaded = true;
            System.out.println("[PaystackCurrencyService] Using defaults: " + CURRENCIES);
        }
    }

    public static List<String> getCurrencies() {
        if (!loaded) return List.of("NGN", "USD", "GBP", "EUR");
        return Collections.unmodifiableList(CURRENCIES);
    }
}
