package com.payguard.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.payguard.service.BankService;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankCacheLoader implements ApplicationRunner {

    private final BankService bankService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Preloading Paystack banks into cache...");
        try {
            bankService.getAllBanks();
            log.info("Banks cache loaded successfully");
        } catch (Exception e) {
            log.error("Failed to preload banks cache", e);
        }
    }
}
