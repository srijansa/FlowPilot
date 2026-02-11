package com.flowpilot.transfer.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferJob {
    private final UUID id;
    private final StorageEndpoint source;
    private final StorageEndpoint destination;
    private final String objectKey;
    private final long objectSizeBytes;
    private final int priority;
    private final int maxRetries;
    private final ChunkPlan chunkPlan;
    private final Instant createdAt;

    private JobStatus status;
    private Instant updatedAt;
    private Instant scheduledAt;
    private final List<TransferAttempt> attempts;

    public TransferJob(
            UUID id,
            StorageEndpoint source,
            StorageEndpoint destination,
            String objectKey,
            long objectSizeBytes,
            int priority,
            int maxRetries,
            ChunkPlan chunkPlan,
            Instant createdAt
    ) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.objectKey = objectKey;
        this.objectSizeBytes = objectSizeBytes;
        this.priority = priority;
        this.maxRetries = maxRetries;
        this.chunkPlan = chunkPlan;
        this.createdAt = createdAt;
        this.status = JobStatus.PENDING;
        this.updatedAt = createdAt;
        this.attempts = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public StorageEndpoint getSource() {
        return source;
    }

    public StorageEndpoint getDestination() {
        return destination;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public long getObjectSizeBytes() {
        return objectSizeBytes;
    }

    public int getPriority() {
        return priority;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public ChunkPlan getChunkPlan() {
        return chunkPlan;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized JobStatus getStatus() {
        return status;
    }

    public synchronized Instant getUpdatedAt() {
        return updatedAt;
    }

    public synchronized Instant getScheduledAt() {
        return scheduledAt;
    }

    public synchronized List<TransferAttempt> getAttempts() {
        return List.copyOf(attempts);
    }

    public synchronized int getAttemptCount() {
        return attempts.size();
    }

    public synchronized void schedule(Instant time) {
        this.scheduledAt = time;
        this.updatedAt = Instant.now();
    }

    public synchronized void markRunning() {
        this.status = JobStatus.RUNNING;
        this.updatedAt = Instant.now();
    }

    public synchronized void markCompleted(TransferAttempt attempt) {
        attempts.add(attempt);
        this.status = JobStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public synchronized void markFailed(TransferAttempt attempt) {
        attempts.add(attempt);
        this.status = JobStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public synchronized void markCancelled() {
        this.status = JobStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
