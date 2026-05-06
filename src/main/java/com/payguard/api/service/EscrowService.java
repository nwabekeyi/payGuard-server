package com.payguard.api.service;

import com.payguard.api.dto.request.*;
import com.payguard.api.dto.response.*;
import com.payguard.api.entity.User;
import com.payguard.api.entity.enumeration.EscrowStatus;

import java.util.List;
import java.util.UUID;

public interface EscrowService {

    EscrowResponse createEscrow(CreateEscrowRequest request, User creator);

    EscrowResponse createAndFundEscrow(CreateEscrowRequest request, User creator, String txnRef, long amountInKobo);

    EscrowResponse getEscrow(UUID id, User currentUser);

    List<EscrowResponse> listEscrows(User currentUser, EscrowStatus statusFilter);

    EscrowResponse transitionStatus(UUID id, StatusTransitionRequest request, User currentUser);

    AuthResult acceptInvite(InviteAcceptRequest request);

    InvitePreviewResponse getInvitePreview(String inviteToken);

    void deleteEscrow(UUID id, DeleteEscrowRequest request, User currentUser);
}
