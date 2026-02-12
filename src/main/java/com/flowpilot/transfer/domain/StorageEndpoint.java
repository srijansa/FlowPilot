package com.flowpilot.transfer.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageEndpoint {
    @NotNull
    @Enumerated(EnumType.STRING)
    private StorageType type;

    @NotBlank
    private String bucket;

    @NotBlank
    private String region;

    private String prefix;

    public StorageEndpoint(StorageType type, String bucket, String region, String prefix) {
        this.type = type;
        this.bucket = bucket;
        this.region = region;
        this.prefix = prefix;
    }
}
