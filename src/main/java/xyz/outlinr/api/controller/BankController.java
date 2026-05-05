package xyz.outlinr.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.outlinr.api.model.AccountDetail;
import xyz.outlinr.api.model.Bank;
import xyz.outlinr.api.service.BankService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping
    public ResponseEntity<List<Bank>> getAllBanks() {
        List<Bank> banks = bankService.getAllBanks();
        return ResponseEntity.ok(banks);
    }

    @GetMapping("/resolve")
    public ResponseEntity<AccountDetail> resolveAccount(
            @RequestParam String account_number,
            @RequestParam String bank_code) {
        AccountDetail detail = bankService.resolveAccount(account_number, bank_code);
        return ResponseEntity.ok(detail);
    }
}
