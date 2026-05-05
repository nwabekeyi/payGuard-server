package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.payguard.api.entity.Escrow;
import com.payguard.api.entity.EscrowStatusHistory;

import java.util.List;
import java.util.UUID;

public interface EscrowStatusHistoryRepository extends JpaRepository<EscrowStatusHistory, UUID> {

    List<EscrowStatusHistory> findByEscrowOrderByTimestampAsc(Escrow escrow);
}
