package com.payguard.service;

import com.payguard.dto.response.AuthResult;
import com.payguard.dto.request.LoginRequest;
import com.payguard.dto.request.RegisterRequest;
import com.payguard.dto.request.AccountSetupRequest;
import com.payguard.dto.request.UpdateBankAccountRequest;
import com.payguard.dto.response.UserResponse;
import com.payguard.entity.User;

public interface AuthService {
    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken);

    UserResponse getCurrentUser();

    AuthResult completeAccountSetup(AccountSetupRequest request, User currentUser);

    UserResponse updateBankAccount(UpdateBankAccountRequest request, User currentUser);
}

