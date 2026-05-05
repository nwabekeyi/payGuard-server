package com.payguard.dto.request;

public record AccountSetupRequest(
    String password,
    String name,
    String accountType
) {}
