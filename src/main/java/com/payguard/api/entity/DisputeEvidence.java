package com.payguard.api.entity;

import jakarta.persistence.*;
import lombok.*;
import com.payguard.api.utils.UUIDv7Generator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_evidence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeEvidence {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "file_url", columnDefinition = "TEXT", nullable = false)
    private String fileUrl;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "uploaded_at", updatable = false)
    private Instant uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUIDv7Generator.generate();
        }
        this.uploadedAt = Instant.now();
    }
}