package com.flowpilot.transfer.domain;

public record ChunkPlan(
        int chunkSizeMb,
        int parallelStreams
) {
}
