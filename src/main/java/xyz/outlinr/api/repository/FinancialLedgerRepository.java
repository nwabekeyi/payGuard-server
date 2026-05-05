package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.payguard.api.entity.FinancialLedger;
import com.payguard.api.entity.Escrow;

import java.util.Optional;
import com.payguard.api.entity.enumeration.LedgerStatus;
import java.util.UUID;

@Repository
public interface FinancialLedgerRepository extends JpaRepository<FinancialLedger, UUID> {
    Optional<FinancialLedger> findByEscrow(Escrow escrow);
    Optional<FinancialLedger> findByPaymentReference(String paymentReference);
    long countByStatus(LedgerStatus status);
}
