package com.flowpilot.transfer.domain;

import jakarta.validation.constraints.NotBlank;

public record StorageEndpoint(
        StorageType type,
        @NotBlank String bucket,
        @NotBlank String region,
        String prefix
) {
}
