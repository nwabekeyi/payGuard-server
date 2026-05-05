package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.payguard.api.entity.PayoutTransaction;
import java.util.UUID;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, UUID> {
}
