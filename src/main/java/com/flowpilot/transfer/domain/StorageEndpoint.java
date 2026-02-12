package com.flowpilot.transfer.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class StorageEndpoint {
    @NotNull
    @Enumerated(EnumType.STRING)
    private StorageType type;

    @NotBlank
    private String bucket;

    @NotBlank
    private String region;

    private String prefix;

    protected StorageEndpoint() {
    }

    public StorageEndpoint(StorageType type, String bucket, String region, String prefix) {
        this.type = type;
        this.bucket = bucket;
        this.region = region;
        this.prefix = prefix;
    }

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
