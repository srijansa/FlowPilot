package com.flowpilot.transfer.service;

import com.flowpilot.transfer.api.CreateTransferJobRequest;
import com.flowpilot.transfer.domain.AttemptStatus;
import com.flowpilot.transfer.domain.JobStatus;
import com.flowpilot.transfer.domain.TransferAttempt;
import com.flowpilot.transfer.domain.TransferJob;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransferJobService {
    private final TransferJobRepository transferJobRepository;

    public TransferJobService(TransferJobRepository transferJobRepository) {
        this.transferJobRepository = transferJobRepository;
    }

    @Transactional
    public TransferJob createJob(CreateTransferJobRequest request) {
        Instant now = Instant.now();
        TransferJob job = new TransferJob(
                UUID.randomUUID(),
                request.source(),
                request.destination(),
                request.objectKey(),
                request.objectSizeBytes(),
                request.priority(),
                request.maxRetries(),
                request.chunkPlan(),
                now
        );
        return transferJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public List<TransferJob> listJobs() {
        return transferJobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public TransferJob getJob(UUID id) {
        return transferJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transfer job not found: " + id));
    }

    @Transactional
    public TransferJob scheduleJob(UUID id, Instant scheduledAt) {
        TransferJob job = getJob(id);
        JobStatus status = job.getStatus();
        if (status == JobStatus.COMPLETED || status == JobStatus.CANCELLED) {
            throw new InvalidTransferStateException("Cannot schedule job in state: " + status);
        }
        job.schedule(scheduledAt);
        return transferJobRepository.save(job);
    }

    @Transactional
    public TransferJob startJob(UUID id, ExecutionOptions options) {
        TransferJob job = getJob(id);
        JobStatus status = job.getStatus();
        if (status != JobStatus.PENDING && status != JobStatus.FAILED) {
            throw new InvalidTransferStateException("Job can only start from PENDING or FAILED. Current state: " + status);
        }
        if (status == JobStatus.FAILED && job.getAttemptCount() > job.getMaxRetries()) {
            throw new InvalidTransferStateException("Retry limit reached for job: " + id);
        }
        execute(job, options);
        return transferJobRepository.save(job);
    }

    @Transactional
    public TransferJob retryJob(UUID id, ExecutionOptions options) {
        TransferJob job = getJob(id);
        JobStatus status = job.getStatus();
        if (status != JobStatus.FAILED) {
            throw new InvalidTransferStateException("Retry is only allowed from FAILED state. Current state: " + status);
        }
        if (job.getAttemptCount() > job.getMaxRetries()) {
            throw new InvalidTransferStateException("Retry limit reached for job: " + id);
        }
        execute(job, options);
        return transferJobRepository.save(job);
    }

    @Transactional
    public TransferJob cancelJob(UUID id) {
        TransferJob job = getJob(id);
        JobStatus status = job.getStatus();
        if (status == JobStatus.COMPLETED || status == JobStatus.CANCELLED) {
            throw new InvalidTransferStateException("Cannot cancel job in state: " + status);
        }
        job.markCancelled();
        return transferJobRepository.save(job);
    }

    private void execute(TransferJob job, ExecutionOptions options) {
        int attemptNumber = job.getAttemptCount() + 1;
        Instant startedAt = Instant.now();
        job.markRunning();

        double throughputMbps = options.throughputMbps() > 0 ? options.throughputMbps() : defaultThroughput(job);
        Instant finishedAt = Instant.now();

        if (options.simulateFailure()) {
            String reason = options.failureReason() == null || options.failureReason().isBlank()
                    ? "Simulated network timeout"
                    : options.failureReason();
            TransferAttempt attempt = new TransferAttempt(
                    attemptNumber,
                    AttemptStatus.FAILED,
                    startedAt,
                    finishedAt,
                    reason,
                    throughputMbps
            );
            job.markFailed(attempt);
            return;
        }

        TransferAttempt attempt = new TransferAttempt(
                attemptNumber,
                AttemptStatus.SUCCESS,
                startedAt,
                finishedAt,
                null,
                throughputMbps
        );
        job.markCompleted(attempt);
    }

    private double defaultThroughput(TransferJob job) {
        double base = 120.0;
        if (job.getObjectSizeBytes() > 5L * 1024 * 1024 * 1024) {
            return base * 0.7;
        }
        if (job.getObjectSizeBytes() < 512L * 1024 * 1024) {
            return base * 1.25;
        }
        return base;
    }
}
