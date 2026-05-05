package com.payguard.api.service;

import java.util.List;

public interface BankService {
    List<com.payguard.api.model.Bank> getAllBanks();
    com.payguard.api.model.AccountDetail resolveAccount(String accountNumber, String bankCode);
}
