package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.AttemptStatus;

public record TransferExecutionResult(
        AttemptStatus status,
        double throughputMbps,
        String failureReason
) {
    public static TransferExecutionResult success(double throughputMbps) {
        return new TransferExecutionResult(AttemptStatus.SUCCESS, throughputMbps, null);
    }

    public static TransferExecutionResult failure(double throughputMbps, String failureReason) {
        return new TransferExecutionResult(AttemptStatus.FAILED, throughputMbps, failureReason);
    }
}
