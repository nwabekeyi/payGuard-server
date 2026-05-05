package com.payguard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.payguard.entity.PayoutTransaction;
import java.util.UUID;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, UUID> {
}
