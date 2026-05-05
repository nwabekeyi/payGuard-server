package com.payguard.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.payguard.api.entity.enumeration.ParticipantRole;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "escrow_participants")
public class EscrowParticipant {

    @Id
    @GeneratedValue(generator = "uuid7")
    @GenericGenerator(name = "uuid7", type = com.payguard.api.utils.UUIDv7IdentifierGenerator.class)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_id", nullable = false)
    private Escrow escrow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Column(nullable = false, unique = true)
    @Builder.Default
    private UUID inviteToken = com.payguard.api.utils.UUIDv7Generator.generate();

    @Column(nullable = false)
    @Builder.Default
    private Boolean inviteAccepted = false;

    private Instant inviteAcceptedAt;
}
