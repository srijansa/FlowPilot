package com.flowpilot.transfer.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "transfer_attempts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    private Instant startedAt;
    private Instant finishedAt;
    private String failureReason;
    private double throughputMbps;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnore
    private TransferJob job;

    public TransferAttempt(
            int attemptNumber,
            AttemptStatus status,
            Instant startedAt,
            Instant finishedAt,
            String failureReason,
            double throughputMbps
    ) {
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failureReason = failureReason;
        this.throughputMbps = throughputMbps;
    }
}
