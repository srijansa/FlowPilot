package com.flowpilot.transfer.service;

import com.flowpilot.transfer.domain.ChunkPlan;
import com.flowpilot.transfer.domain.StorageEndpoint;
import com.flowpilot.transfer.domain.StorageType;
import com.flowpilot.transfer.domain.TransferJob;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@ConditionalOnProperty(name = "transfer.executor.mode", havingValue = "s3-minio")
public class S3MinioTransferExecutor implements TransferExecutor {
    private static final long MIN_MULTIPART_SIZE_BYTES = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final S3Client minioClient;

    public S3MinioTransferExecutor(TransferStorageProperties properties) {
        this.s3Client = buildClient(properties.getS3());
        this.minioClient = buildClient(properties.getMinio());
    }

    @Override
    public TransferExecutionResult execute(TransferJob job, ExecutionOptions options) {
        if (options.simulateFailure()) {
            String reason = options.failureReason() == null || options.failureReason().isBlank()
                    ? "Simulated transfer failure"
                    : options.failureReason();
            return TransferExecutionResult.failure(0, reason);
        }

        StorageEndpoint source = job.getSource();
        StorageEndpoint destination = job.getDestination();
        String sourceKey = objectKey(source, job.getObjectKey());
        String destinationKey = objectKey(destination, job.getObjectKey());
        S3Client sourceClient = clientFor(source.getType());
        S3Client destinationClient = clientFor(destination.getType());

        Instant start = Instant.now();
        try {
            HeadObjectResponse head = sourceClient.headObject(
                    HeadObjectRequest.builder().bucket(source.getBucket()).key(sourceKey).build()
            );
            long objectSizeBytes = head.contentLength();
            long partSizeBytes = resolvePartSize(job.getChunkPlan());
            int parallelStreams = resolveParallelStreams(job.getChunkPlan());

            if (objectSizeBytes <= partSizeBytes) {
                transferSinglePart(sourceClient, destinationClient, source.getBucket(), destination.getBucket(), sourceKey, destinationKey, objectSizeBytes);
            } else {
                transferMultipart(
                        sourceClient,
                        destinationClient,
                        source.getBucket(),
                        destination.getBucket(),
                        sourceKey,
                        destinationKey,
                        objectSizeBytes,
                        partSizeBytes,
                        parallelStreams
                );
            }

            double throughputMbps = throughput(objectSizeBytes, Duration.between(start, Instant.now()));
            return TransferExecutionResult.success(throughputMbps);
        } catch (S3Exception | IOException ex) {
            return TransferExecutionResult.failure(0, "Transfer failed: " + ex.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        s3Client.close();
        minioClient.close();
    }

    private void transferSinglePart(
            S3Client sourceClient,
            S3Client destinationClient,
            String sourceBucket,
            String destinationBucket,
            String sourceKey,
            String destinationKey,
            long objectSizeBytes
    ) throws IOException {
        try (ResponseInputStream<GetObjectResponse> stream = sourceClient.getObject(
                GetObjectRequest.builder().bucket(sourceBucket).key(sourceKey).build())) {
            destinationClient.putObject(
                    PutObjectRequest.builder().bucket(destinationBucket).key(destinationKey).build(),
                    RequestBody.fromInputStream(stream, objectSizeBytes)
            );
        }
    }

    private void transferMultipart(
            S3Client sourceClient,
            S3Client destinationClient,
            String sourceBucket,
            String destinationBucket,
            String sourceKey,
            String destinationKey,
            long objectSizeBytes,
            long partSizeBytes,
            int parallelStreams
    ) {
        CreateMultipartUploadResponse multipart = destinationClient.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(destinationBucket)
                        .key(destinationKey)
                        .build()
        );
        String uploadId = multipart.uploadId();
        ExecutorService executor = Executors.newFixedThreadPool(parallelStreams);

        try {
            int totalParts = (int) ((objectSizeBytes + partSizeBytes - 1) / partSizeBytes);
            List<Future<CompletedPart>> futures = new ArrayList<>(totalParts);
            for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                long startByte = (long) (partNumber - 1) * partSizeBytes;
                long endByte = Math.min(objectSizeBytes - 1, startByte + partSizeBytes - 1);
                long length = endByte - startByte + 1;

                futures.add(executor.submit(uploadPartTask(
                        sourceClient,
                        destinationClient,
                        sourceBucket,
                        destinationBucket,
                        sourceKey,
                        destinationKey,
                        uploadId,
                        partNumber,
                        startByte,
                        endByte,
                        length
                )));
            }

            List<CompletedPart> completedParts = new ArrayList<>(totalParts);
            for (Future<CompletedPart> future : futures) {
                try {
                    completedParts.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transfer interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Multipart part upload failed", e.getCause());
                }
            }
            completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

            destinationClient.completeMultipartUpload(
                    CompleteMultipartUploadRequest.builder()
                            .bucket(destinationBucket)
                            .key(destinationKey)
                            .uploadId(uploadId)
                            .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                            .build()
            );
        } catch (RuntimeException ex) {
            destinationClient.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                            .bucket(destinationBucket)
                            .key(destinationKey)
                            .uploadId(uploadId)
                            .build()
            );
            throw ex;
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<CompletedPart> uploadPartTask(
            S3Client sourceClient,
            S3Client destinationClient,
            String sourceBucket,
            String destinationBucket,
            String sourceKey,
            String destinationKey,
            String uploadId,
            int partNumber,
            long startByte,
            long endByte,
            long contentLength
    ) {
        return () -> {
            String range = "bytes=" + startByte + "-" + endByte;
            try (ResponseInputStream<GetObjectResponse> stream = sourceClient.getObject(
                    GetObjectRequest.builder()
                            .bucket(sourceBucket)
                            .key(sourceKey)
                            .range(range)
                            .build()
            )) {
                UploadPartResponse response = destinationClient.uploadPart(
                        UploadPartRequest.builder()
                                .bucket(destinationBucket)
                                .key(destinationKey)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .contentLength(contentLength)
                                .build(),
                        RequestBody.fromInputStream(stream, contentLength)
                );
                return CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(response.eTag())
                        .build();
            }
        };
    }

    private S3Client buildClient(TransferStorageProperties.Backend config) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(config.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(config.isPathStyleAccess())
                        .build());

        if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(config.getEndpoint()));
        }

        if (hasText(config.getAccessKey()) && hasText(config.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private S3Client clientFor(StorageType type) {
        return type == StorageType.MINIO ? minioClient : s3Client;
    }

    private long resolvePartSize(ChunkPlan chunkPlan) {
        int chunkMb = chunkPlan != null ? chunkPlan.getChunkSizeMb() : 64;
        long configured = Math.max(chunkMb, 1) * 1024L * 1024L;
        return Math.max(configured, MIN_MULTIPART_SIZE_BYTES);
    }

    private int resolveParallelStreams(ChunkPlan chunkPlan) {
        int streams = chunkPlan != null ? chunkPlan.getParallelStreams() : 4;
        return Math.max(streams, 1);
    }

    private String objectKey(StorageEndpoint endpoint, String objectKey) {
        if (!hasText(endpoint.getPrefix())) {
            return objectKey;
        }
        return trimSlash(endpoint.getPrefix()) + "/" + trimSlash(objectKey);
    }

    private String trimSlash(String value) {
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double throughput(long bytes, Duration elapsed) {
        double seconds = Math.max(elapsed.toMillis() / 1000.0, 0.001);
        return (bytes * 8.0) / 1_000_000.0 / seconds;
    }
}
