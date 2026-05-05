package com.payguard.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.payguard.api.entity.DisputeEvidence;

import java.util.UUID;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, UUID> {
}
