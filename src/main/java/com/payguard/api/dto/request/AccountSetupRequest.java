package com.payguard.api.dto.request;

public record AccountSetupRequest(
    String password,
    String name,
    String accountType
) {}
