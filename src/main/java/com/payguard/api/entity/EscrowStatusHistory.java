package com.payguard.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.payguard.api.entity.enumeration.EscrowStatus;
import com.payguard.api.utils.UUIDv7Generator;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "escrow_status_history")
public class EscrowStatusHistory {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_id", nullable = false)
    private Escrow escrow;

    @Enumerated(EnumType.STRING)
    private EscrowStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUIDv7Generator.generate();
        }
    }
}
