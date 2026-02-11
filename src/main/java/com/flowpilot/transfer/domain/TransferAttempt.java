package com.flowpilot.transfer.domain;

import java.time.Instant;

public final class TransferAttempt {
    private final int attemptNumber;
    private final AttemptStatus status;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String failureReason;
    private final double throughputMbps;

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

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public double getThroughputMbps() {
        return throughputMbps;
    }
}
