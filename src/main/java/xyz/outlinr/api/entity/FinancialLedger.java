package com.payguard.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.payguard.api.entity.enumeration.LedgerStatus;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "financial_ledger")
public class FinancialLedger {

    @Id
    @GeneratedValue(generator = "uuid7")
    @GenericGenerator(name = "uuid7", type = com.payguard.api.utils.UUIDv7IdentifierGenerator.class)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_id", nullable = false)
    private Escrow escrow;

    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "paystack_fee", nullable = false)
    @Builder.Default
    private BigDecimal paystackFee = BigDecimal.ZERO;
    @Column(name = "platform_fee", nullable = false)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;
    @Column(name = "net_payout_amount", nullable = false)
    @Builder.Default
    private BigDecimal netPayoutAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerStatus status;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
