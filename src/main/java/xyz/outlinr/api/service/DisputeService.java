package com.payguard.api.service;

import org.springframework.web.multipart.MultipartFile;
import com.payguard.api.dto.request.CreateDisputeRequest;
import com.payguard.api.dto.request.ResolveDisputeRequest;
import com.payguard.api.dto.response.DisputeEvidenceResponse;
import com.payguard.api.dto.response.DisputeResponse;
import com.payguard.api.entity.User;

import java.util.List;
import java.util.UUID;

public interface DisputeService {

    DisputeResponse raiseDispute(UUID escrowId, CreateDisputeRequest request, User currentUser);

    DisputeResponse getDispute(UUID disputeId, User currentUser);

    DisputeResponse getDisputeByEscrow(UUID escrowId, User currentUser);

    List<DisputeResponse> listUserDisputes(User currentUser);

    List<DisputeResponse> listAllDisputes();

    DisputeEvidenceResponse uploadEvidence(UUID disputeId, MultipartFile file, User currentUser);

    DisputeResponse resolveDispute(UUID disputeId, ResolveDisputeRequest request, User adminUser);

    void autoResolveExpiredDisputes();
}
