package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.payguard.api.entity.Escrow;
import com.payguard.api.entity.enumeration.EscrowStatus;
import com.payguard.api.entity.User;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface EscrowRepository extends JpaRepository<Escrow, UUID> {

    @Query("SELECT DISTINCT e FROM Escrow e JOIN e.participants p WHERE p.user = :user")
    List<Escrow> findByParticipantUser(@Param("user") User user);

    @Query("SELECT DISTINCT e FROM Escrow e JOIN e.participants p WHERE p.user = :user AND e.status = :status")
    List<Escrow> findByParticipantUserAndStatus(@Param("user") User user, @Param("status") EscrowStatus status);

    long countByStatus(EscrowStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Escrow e WHERE e.id = :id")
    java.util.Optional<Escrow> findByIdForUpdate(@Param("id") UUID id);
}
