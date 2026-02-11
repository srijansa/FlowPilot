package com.flowpilot.transfer.api;

public record ExecuteTransferRequest(
        boolean simulateFailure,
        double throughputMbps,
        String failureReason
) {
}
