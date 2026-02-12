package com.flowpilot.transfer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfer_jobs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferJob {
    @Id
    private UUID id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "source_type")),
            @AttributeOverride(name = "bucket", column = @Column(name = "source_bucket")),
            @AttributeOverride(name = "region", column = @Column(name = "source_region")),
            @AttributeOverride(name = "prefix", column = @Column(name = "source_prefix"))
    })
    private StorageEndpoint source;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "destination_type")),
            @AttributeOverride(name = "bucket", column = @Column(name = "destination_bucket")),
            @AttributeOverride(name = "region", column = @Column(name = "destination_region")),
            @AttributeOverride(name = "prefix", column = @Column(name = "destination_prefix"))
    })
    private StorageEndpoint destination;

    private String objectKey;
    private long objectSizeBytes;
    private int priority;
    private int maxRetries;

    @Embedded
    private ChunkPlan chunkPlan;

    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private Instant updatedAt;
    private Instant scheduledAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("attemptNumber ASC")
    private List<TransferAttempt> attempts = new ArrayList<>();

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
    }

    public synchronized JobStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(JobStatus status) {
        this.status = status;
    }

    public synchronized Instant getUpdatedAt() {
        return updatedAt;
    }

    public synchronized void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public synchronized Instant getScheduledAt() {
        return scheduledAt;
    }

    public synchronized void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
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
        attempt.setJob(this);
        attempts.add(attempt);
        this.status = JobStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public synchronized void markFailed(TransferAttempt attempt) {
        attempt.setJob(this);
        attempts.add(attempt);
        this.status = JobStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public synchronized void markCancelled() {
        this.status = JobStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
