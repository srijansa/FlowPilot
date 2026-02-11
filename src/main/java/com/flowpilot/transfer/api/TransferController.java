package com.flowpilot.transfer.api;

import com.flowpilot.transfer.domain.TransferJob;
import com.flowpilot.transfer.service.ExecutionOptions;
import com.flowpilot.transfer.service.TransferJobService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Validated
public class TransferController {
    private final TransferJobService transferJobService;

    public TransferController(TransferJobService transferJobService) {
        this.transferJobService = transferJobService;
    }

    @PostMapping
    public TransferJob createTransfer(@Valid @RequestBody CreateTransferJobRequest request) {
        return transferJobService.createJob(request);
    }

    @GetMapping
    public List<TransferJob> listTransfers() {
        return transferJobService.listJobs();
    }

    @GetMapping("/{jobId}")
    public TransferJob getTransfer(@PathVariable UUID jobId) {
        return transferJobService.getJob(jobId);
    }

    @PostMapping("/{jobId}/schedule")
    public TransferJob scheduleTransfer(@PathVariable UUID jobId, @RequestBody(required = false) ScheduleTransferRequest request) {
        Instant scheduledAt = request != null && request.scheduledAt() != null ? request.scheduledAt() : Instant.now();
        return transferJobService.scheduleJob(jobId, scheduledAt);
    }

    @PostMapping("/{jobId}/start")
    public TransferJob startTransfer(@PathVariable UUID jobId, @RequestBody(required = false) ExecuteTransferRequest request) {
        return transferJobService.startJob(jobId, toExecutionOptions(request));
    }

    @PostMapping("/{jobId}/retry")
    public TransferJob retryTransfer(@PathVariable UUID jobId, @RequestBody(required = false) ExecuteTransferRequest request) {
        return transferJobService.retryJob(jobId, toExecutionOptions(request));
    }

    @PostMapping("/{jobId}/cancel")
    public TransferJob cancelTransfer(@PathVariable UUID jobId) {
        return transferJobService.cancelJob(jobId);
    }

    private ExecutionOptions toExecutionOptions(ExecuteTransferRequest request) {
        if (request == null) {
            return ExecutionOptions.success(0);
        }
        return new ExecutionOptions(request.simulateFailure(), request.throughputMbps(), request.failureReason());
    }
}
