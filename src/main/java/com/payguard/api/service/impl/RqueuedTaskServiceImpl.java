package com.payguard.api.service.impl;

import com.github.rahulrishi.rqueue.annotation.RqueueListener;
import com.github.rahulrishi.rqueue.core.RqueueMessage;
import com.github.rahulrishi.rqueue.spring.RqueueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.payguard.api.entity.Dispute;
import com.payguard.api.service.CriticalTaskQueueService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RqueuedTaskServiceImpl implements CriticalTaskQueueService {

    private final RqueueProducer rqueueProducer;

    @Override
    public void enqueueDisputeResolvedTask(Dispute dispute) {
        DisputeIdMessage message = new DisputeIdMessage(
                dispute.getId(),
                dispute.getEscrow().getId()
        );
        rqueueProducer.enqueue("dispute-resolved-queue", message);
        log.info("[rqueue] enqueued resolved dispute task for disputeId={} escrowId={}",
                dispute.getId(), dispute.getEscrow().getId());
    }

    @RqueueListener(value = "dispute-resolved-queue")
    public void processDisputeResolvedTask(RqueueMessage<DisputeIdMessage> message) {
        DisputeIdMessage payload = message.getMessage();
        log.info("[rqueue] processing resolved dispute task for disputeId={} escrowId={}",
                payload.disputeId(), payload.escrowId());
    }

    public record DisputeIdMessage(UUID disputeId, UUID escrowId) {}
}