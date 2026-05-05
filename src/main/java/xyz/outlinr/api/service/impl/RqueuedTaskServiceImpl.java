package com.payguard.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.payguard.entity.Dispute;
import com.payguard.service.CriticalTaskQueueService;

@Slf4j
@Service
public class RqueuedTaskServiceImpl implements CriticalTaskQueueService {

    @Override
    @Async("taskExecutor")
    public void enqueueDisputeResolvedTask(Dispute dispute) {
        // queue-style async worker hook for critical post-resolution operations
        log.info("[rqueued] processing resolved dispute task for disputeId={} escrowId={}",
                dispute.getId(), dispute.getEscrow().getId());
    }
}
