package com.payguard.dto.request;

import java.util.UUID;

public record InviteAcceptRequest(
        UUID inviteToken) {
}
