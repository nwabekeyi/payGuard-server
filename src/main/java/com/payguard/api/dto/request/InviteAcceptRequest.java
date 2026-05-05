package com.payguard.api.dto.request;

import java.util.UUID;

public record InviteAcceptRequest(
        UUID inviteToken) {
}
