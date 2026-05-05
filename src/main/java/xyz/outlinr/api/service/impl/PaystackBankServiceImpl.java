package xyz.outlinr.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.outlinr.api.config.PaystackConfig;
import xyz.outlinr.api.model.AccountDetail;
import xyz.outlinr.api.model.Bank;
import xyz.outlinr.api.service.BankService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackBankServiceImpl implements BankService {

    private final PaystackConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.banks.cache.ttl-seconds:86400}")
    private long cacheTtlSeconds;

    private static final String BANKS_CACHE_KEY = "paystack:banks";

    @Override
    public List<Bank> getAllBanks() {
        try {
            Object cached = redisTemplate.opsForValue().get(BANKS_CACHE_KEY);
            if (cached instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Bank> banks = (List<Bank>) cached;
                if (!banks.isEmpty()) {
                    log.debug("Returning banks from cache (size: {})", banks.size());
                    return banks;
                }
            }
        } catch (Exception e) {
            log.warn("Redis cache miss or error, fetching from Paystack", e);
        }

        List<Bank> banks = fetchBanksFromPaystack();

        try {
            redisTemplate.opsForValue().set(
                    BANKS_CACHE_KEY,
                    banks,
                    Duration.ofSeconds(cacheTtlSeconds)
            );
            log.info("Cached {} banks in Redis", banks.size());
        } catch (Exception e) {
            log.error("Failed to cache banks in Redis", e);
        }

        return banks;
    }

    private List<Bank> fetchBanksFromPaystack() {
        String url = config.getBaseUrl() + "/bank";
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
            if (body != null && Boolean.TRUE.equals(body.get("status")) && body.get("data") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> banks = (List<Map<String, Object>>) body.get("data");
                return banks.stream()
                        .map(b -> new Bank((String) b.get("code"), (String) b.get("name")))
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch banks from Paystack", e);
            throw new RuntimeException("Could not retrieve banks", e);
        }
    }

    @Override
    public AccountDetail resolveAccount(String accountNumber, String bankCode) {
        String url = String.format("%s/bank/resolve?account_number=%s&bank_code=%s",
                config.getBaseUrl(), accountNumber, bankCode);
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
                String accNum = (String) data.get("account_number");
                String accName = (String) data.get("account_name");
                return new AccountDetail(accNum, accName);
            }
            throw new RuntimeException("Account resolution failed: " + body);
        } catch (Exception e) {
            log.error("Failed to resolve account via Paystack", e);
            throw new RuntimeException("Could not verify account details", e);
        }
    }
}
