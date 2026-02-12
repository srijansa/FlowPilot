package com.flowpilot.transfer.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChunkPlan {
    @Min(1)
    private int chunkSizeMb;

    @Min(1)
    private int parallelStreams;

    public ChunkPlan(int chunkSizeMb, int parallelStreams) {
        this.chunkSizeMb = chunkSizeMb;
        this.parallelStreams = parallelStreams;
    }
}
