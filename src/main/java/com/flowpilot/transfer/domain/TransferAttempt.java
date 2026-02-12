package com.flowpilot.transfer.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transfer_attempts")
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

    protected TransferAttempt() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public double getThroughputMbps() {
        return throughputMbps;
    }

    public void setThroughputMbps(double throughputMbps) {
        this.throughputMbps = throughputMbps;
    }

    public TransferJob getJob() {
        return job;
    }

    public void setJob(TransferJob job) {
        this.job = job;
    }
}
