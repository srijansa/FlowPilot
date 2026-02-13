package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.TransferJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "transfer.executor.mode", havingValue = "s3-minio")
public class S3MinioTransferExecutor implements TransferExecutor {

    @Override
    public TransferExecutionResult execute(TransferJob job, ExecutionOptions options) {
        return TransferExecutionResult.failure(0, "s3-minio executor is not implemented yet");
    }
}
