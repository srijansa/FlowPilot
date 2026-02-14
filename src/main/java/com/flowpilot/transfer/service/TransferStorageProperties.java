package com.flowpilot.transfer.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transfer")
@Getter
@Setter
public class TransferStorageProperties {
    private Executor executor = new Executor();
    private Backend s3 = new Backend();
    private Backend minio = new Backend();

    @Getter
    @Setter
    public static class Executor {
        private String mode = "simulated";
    }

    @Getter
    @Setter
    public static class Backend {
        private String endpoint;
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess = false;
    }
}
