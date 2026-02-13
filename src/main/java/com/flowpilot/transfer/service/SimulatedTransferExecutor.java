package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.TransferJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "transfer.executor.mode", havingValue = "simulated", matchIfMissing = true)
public class SimulatedTransferExecutor implements TransferExecutor {

    @Override
    public TransferExecutionResult execute(TransferJob job, ExecutionOptions options) {
        double throughputMbps = options.throughputMbps() > 0 ? options.throughputMbps() : defaultThroughput(job.getObjectSizeBytes());

        if (options.simulateFailure()) {
            String reason = options.failureReason() == null || options.failureReason().isBlank()
                    ? "Simulated network timeout"
                    : options.failureReason();
            return TransferExecutionResult.failure(throughputMbps, reason);
        }

        return TransferExecutionResult.success(throughputMbps);
    }

    private double defaultThroughput(long objectSizeBytes) {
        double base = 120.0;
        if (objectSizeBytes > 5L * 1024 * 1024 * 1024) {
            return base * 0.7;
        }
        if (objectSizeBytes < 512L * 1024 * 1024) {
            return base * 1.25;
        }
        return base;
    }
}
