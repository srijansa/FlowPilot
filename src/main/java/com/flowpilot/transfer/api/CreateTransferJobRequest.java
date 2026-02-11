package com.flowpilot.transfer.api;

import com.flowpilot.transfer.domain.ChunkPlan;
import com.flowpilot.transfer.domain.StorageEndpoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTransferJobRequest(
        @Valid @NotNull StorageEndpoint source,
        @Valid @NotNull StorageEndpoint destination,
        @NotBlank String objectKey,
        @Positive long objectSizeBytes,
        @Min(1) @Max(10) int priority,
        @Min(0) @Max(10) int maxRetries,
        @Valid @NotNull ChunkPlan chunkPlan
) {
}
