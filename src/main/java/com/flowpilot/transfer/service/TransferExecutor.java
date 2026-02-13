package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.TransferJob;

public interface TransferExecutor {
    TransferExecutionResult execute(TransferJob job, ExecutionOptions options);
}
