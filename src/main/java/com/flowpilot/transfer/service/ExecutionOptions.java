package com.flowpilot.transfer.service;

public record ExecutionOptions(
        boolean simulateFailure,
        double throughputMbps,
        String failureReason
) {
    public static ExecutionOptions success(double throughputMbps) {
        return new ExecutionOptions(false, throughputMbps, null);
    }
}
