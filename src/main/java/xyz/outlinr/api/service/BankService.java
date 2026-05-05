package com.payguard.service;

import java.util.List;

public interface BankService {
    List<com.payguard.model.Bank> getAllBanks();
    com.payguard.model.AccountDetail resolveAccount(String accountNumber, String bankCode);
}
