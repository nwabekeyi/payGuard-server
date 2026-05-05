package com.payguard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payguard.dto.request.ResolveDisputeRequest;
import com.payguard.dto.response.AdminDashboardResponse;
import com.payguard.dto.response.DisputeResponse;
import com.payguard.entity.User;
import com.payguard.entity.enumeration.DisputeStatus;
import com.payguard.entity.enumeration.EscrowStatus;
import com.payguard.entity.enumeration.LedgerStatus;
import com.payguard.repository.DisputeRepository;
import com.payguard.repository.EscrowRepository;
import com.payguard.repository.FinancialLedgerRepository;
import com.payguard.service.AdminEscrowService;
import com.payguard.service.DisputeService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEscrowServiceImpl implements AdminEscrowService {
    private final EscrowRepository escrowRepository;
    private final DisputeRepository disputeRepository;
    private final FinancialLedgerRepository ledgerRepository;
    private final DisputeService disputeService;

    @Override
    @Cacheable(value = "adminDashboard", key = "'summary'", sync = true)
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                escrowRepository.count(),
                escrowRepository.countByStatus(EscrowStatus.ACTIVE),
                escrowRepository.countByStatus(EscrowStatus.DISPUTED),
                disputeRepository.countByStatus(DisputeStatus.OPEN),
                disputeRepository.countByStatus(DisputeStatus.RESOLVED),
                ledgerRepository.countByStatus(LedgerStatus.PENDING_PAYOUT)
        );
    }

    @Override
    @CacheEvict(value = "adminDashboard", allEntries = true)
    public DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, User adminUser) {
        return disputeService.resolveDispute(disputeId, request, adminUser);
    }
}
