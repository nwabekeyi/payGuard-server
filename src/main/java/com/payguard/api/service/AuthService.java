package com.payguard.api.service;

import com.payguard.api.dto.response.AuthResult;
import com.payguard.api.dto.request.LoginRequest;
import com.payguard.api.dto.request.RegisterRequest;
import com.payguard.api.dto.request.AccountSetupRequest;
import com.payguard.api.dto.request.UpdateBankAccountRequest;
import com.payguard.api.dto.response.UserResponse;
import com.payguard.api.entity.User;

public interface AuthService {
    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken);

    UserResponse getCurrentUser();

    AuthResult completeAccountSetup(AccountSetupRequest request, User currentUser);

    UserResponse updateBankAccount(UpdateBankAccountRequest request, User currentUser);
}

