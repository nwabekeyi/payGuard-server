package com.payguard.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import com.payguard.api.entity.enumeration.KycType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_kyc")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserKyc {
    @Id
    @GeneratedValue(generator = "uuid7")
    @GenericGenerator(name = "uuid7", type = com.payguard.api.utils.UUIDv7IdentifierGenerator.class)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_type", nullable = false)
    private KycType kycType;

    @Column(name = "kyc_id", nullable = false, length = 32)
    private String kycId;

    @Column(name = "bank_account_number", nullable = false, length = 16)
    private String bankAccountNumber;

    @Column(name = "bank_code", nullable = false, length = 16)
    private String bankCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "verification_reference")
    private String verificationReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate(){ this.updatedAt = Instant.now(); }
}
