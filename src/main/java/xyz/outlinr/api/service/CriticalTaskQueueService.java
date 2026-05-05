package com.payguard.service;

import com.payguard.entity.Dispute;

public interface CriticalTaskQueueService {
    void enqueueDisputeResolvedTask(Dispute dispute);
}
