package com.payguard.api.service.impl;

import com.payguard.api.entity.Dispute;
import com.payguard.api.service.CriticalTaskQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonTaskQueueServiceImpl implements CriticalTaskQueueService {

    private static final String DISPUTE_RESOLVED_QUEUE = "dispute-resolved-queue";

    private final RedissonClient redissonClient;

    @Override
    public void enqueueDisputeResolvedTask(Dispute dispute) {
        DisputeIdMessage message = new DisputeIdMessage(
                dispute.getId(),
                dispute.getEscrow().getId()
        );

        RBlockingQueue<DisputeIdMessage> queue = redissonClient.getBlockingQueue(DISPUTE_RESOLVED_QUEUE);
        queue.add(message);
        log.info("[redisson] enqueued resolved dispute task for disputeId={} escrowId={}",
                dispute.getId(), dispute.getEscrow().getId());
    }

    @Scheduled(fixedDelayString = "${tasks.dispute-resolved-consumer-delay-ms:1000}")
    public void processDisputeResolvedTask() {
        RBlockingQueue<DisputeIdMessage> queue = redissonClient.getBlockingQueue(DISPUTE_RESOLVED_QUEUE);
        DisputeIdMessage payload = queue.poll();

        if (payload == null) {
            return;
        }

        log.info("[redisson] processing resolved dispute task for disputeId={} escrowId={}",
                payload.disputeId(), payload.escrowId());
    }

    public record DisputeIdMessage(UUID disputeId, UUID escrowId) {
    }
}
