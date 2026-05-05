package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.payguard.api.entity.User;
import com.payguard.api.entity.UserKyc;

import java.util.Optional;
import java.util.UUID;

public interface UserKycRepository extends JpaRepository<UserKyc, UUID> {
    Optional<UserKyc> findByUser(User user);
}
