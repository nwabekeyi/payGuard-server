package com.payguard.api.service;

import com.payguard.api.entity.Dispute;

public interface CriticalTaskQueueService {
    void enqueueDisputeResolvedTask(Dispute dispute);
}
