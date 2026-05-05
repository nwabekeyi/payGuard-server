package xyz.outlinr.api.service;

import java.util.List;

public interface BankService {
    List<xyz.outlinr.api.model.Bank> getAllBanks();
    xyz.outlinr.api.model.AccountDetail resolveAccount(String accountNumber, String bankCode);
}
