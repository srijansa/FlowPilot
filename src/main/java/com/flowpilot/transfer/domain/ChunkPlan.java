package com.flowpilot.transfer.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;

@Embeddable
public class ChunkPlan {
    @Min(1)
    private int chunkSizeMb;

    @Min(1)
    private int parallelStreams;

    protected ChunkPlan() {
    }

    public ChunkPlan(int chunkSizeMb, int parallelStreams) {
        this.chunkSizeMb = chunkSizeMb;
        this.parallelStreams = parallelStreams;
    }

    public int getChunkSizeMb() {
        return chunkSizeMb;
    }

    public void setChunkSizeMb(int chunkSizeMb) {
        this.chunkSizeMb = chunkSizeMb;
    }

    public int getParallelStreams() {
        return parallelStreams;
    }

    public void setParallelStreams(int parallelStreams) {
        this.parallelStreams = parallelStreams;
    }
}
