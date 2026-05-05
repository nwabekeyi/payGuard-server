package com.payguard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.payguard.entity.Escrow;
import com.payguard.entity.EscrowStatusHistory;

import java.util.List;
import java.util.UUID;

public interface EscrowStatusHistoryRepository extends JpaRepository<EscrowStatusHistory, UUID> {

    List<EscrowStatusHistory> findByEscrowOrderByTimestampAsc(Escrow escrow);
}
